package com.somepro.infrastructure.persistence.inventory;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.somepro.common.exception.BizException;
import com.somepro.domain.inventory.model.BatchStatus;
import com.somepro.domain.inventory.model.ManifestStatus;
import com.somepro.domain.inventory.model.TransferManifest;
import com.somepro.domain.inventory.model.WasteStock;
import com.somepro.domain.inventory.repository.InventoryRepository;
import com.somepro.domain.stockcheck.model.CheckStatus;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.inventory.converter.TransferManifestPoConverter;
import com.somepro.infrastructure.persistence.inventory.converter.WasteStockPoConverter;
import com.somepro.infrastructure.persistence.inventory.po.TransferManifestPO;
import com.somepro.infrastructure.persistence.inventory.po.TransferPlanPO;
import com.somepro.infrastructure.persistence.inventory.po.WasteCategoryPO;
import com.somepro.infrastructure.persistence.inventory.po.WasteStockPO;
import com.somepro.infrastructure.persistence.stockcheck.StockCheckMapper;
import com.somepro.infrastructure.persistence.stockcheck.po.StockCheckPO;
import com.somepro.infrastructure.persistence.support.ComboLockRegistry;
import com.somepro.infrastructure.persistence.support.DocNoGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * 库存仓储适配器（基础设施层）：新入库与联单转出。
 *
 * 盘点冻结的实现约定：入库与转出全程持有「单位+类别」组合锁，锁内先查该组合是否存在
 * 盘点占用态（COUNTING/PENDING_APPROVAL/APPROVED）的盘点单，有则拒动账；
 * 盘点单状态流转（StockCheckRepositoryImpl）持同一把锁，因此开工/调账/作废与进出库严格互斥。
 * 没在盘点中的组合照常放行，不误伤。
 */
@Repository
public class InventoryRepositoryImpl implements InventoryRepository {

    /** 取号撞唯一键时的最大重试次数。 */
    private static final int NO_RETRY = 3;

    private final WasteStockMapper wasteStockMapper;
    private final TransferManifestMapper transferManifestMapper;
    private final TransferPlanMapper transferPlanMapper;
    private final WasteSourceMapper wasteSourceMapper;
    private final WasteCategoryMapper wasteCategoryMapper;
    private final TreatmentUnitMapper treatmentUnitMapper;
    private final StockCheckMapper stockCheckMapper;
    private final DocNoGenerator docNoGenerator;
    private final ComboLockRegistry comboLockRegistry;
    private final TransactionTemplate txTemplate;

    public InventoryRepositoryImpl(WasteStockMapper wasteStockMapper,
                                   TransferManifestMapper transferManifestMapper,
                                   TransferPlanMapper transferPlanMapper,
                                   WasteSourceMapper wasteSourceMapper,
                                   WasteCategoryMapper wasteCategoryMapper,
                                   TreatmentUnitMapper treatmentUnitMapper,
                                   StockCheckMapper stockCheckMapper,
                                   DocNoGenerator docNoGenerator,
                                   ComboLockRegistry comboLockRegistry,
                                   PlatformTransactionManager txManager) {
        this.wasteStockMapper = wasteStockMapper;
        this.transferManifestMapper = transferManifestMapper;
        this.transferPlanMapper = transferPlanMapper;
        this.wasteSourceMapper = wasteSourceMapper;
        this.wasteCategoryMapper = wasteCategoryMapper;
        this.treatmentUnitMapper = treatmentUnitMapper;
        this.stockCheckMapper = stockCheckMapper;
        this.docNoGenerator = docNoGenerator;
        this.comboLockRegistry = comboLockRegistry;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public Mono<Boolean> existsSource(Long sourceId) {
        return blocking(() -> sourceId != null && wasteSourceMapper.selectById(sourceId) != null);
    }

    @Override
    public Mono<Boolean> existsCategory(String categoryCode) {
        return blocking(() -> categoryCode != null && wasteCategoryMapper.selectCount(
                Wrappers.lambdaQuery(WasteCategoryPO.class)
                        .eq(WasteCategoryPO::getCategoryCode, categoryCode)) > 0);
    }

    @Override
    public Mono<Boolean> existsUnit(Long unitId) {
        return blocking(() -> unitId != null && treatmentUnitMapper.selectById(unitId) != null);
    }

    @Override
    public Mono<BigDecimal> sumInStockWeight(Long sourceId, String categoryCode) {
        return blocking(() -> sumInStock(sourceId, categoryCode));
    }

    @Override
    public Mono<WasteStock> stockIn(WasteStock stock) {
        return blocking(() -> comboLockRegistry.withLock(
                comboLockRegistry.inventoryKey(stock.getSourceId(), stock.getCategoryCode()),
                () -> {
                    rejectIfFrozen(stock.getSourceId(), stock.getCategoryCode(), "入库");
                    WasteStockPO po = WasteStockPoConverter.toPo(stock);
                    po.setId(IdUtil.getSnowflakeNextId());
                    for (int i = 0; i < NO_RETRY; i++) {
                        po.setBatchNo(docNoGenerator.next("WB", this::maxBatchNoOfYear));
                        try {
                            wasteStockMapper.insert(po);
                            return WasteStockPoConverter.toDomain(po);
                        } catch (DuplicateKeyException e) {
                            if (i == NO_RETRY - 1) {
                                throw new BizException("入库批次编号分配冲突，请重试");
                            }
                        }
                    }
                    throw new BizException("入库批次编号分配冲突，请重试");
                }));
    }

    @Override
    public Mono<TransferManifest> transferOut(TransferManifest manifest) {
        return blocking(() -> comboLockRegistry.withLock(
                comboLockRegistry.inventoryKey(manifest.getSourceId(), manifest.getCategoryCode()),
                () -> txTemplate.execute(tx -> {
                    // 1) 冻结检查：盘点占用中的组合先停下来，等调完账或作废再放行
                    rejectIfFrozen(manifest.getSourceId(), manifest.getCategoryCode(), "联单转出");
                    // 2) 库存校验：在库合计不足不许转
                    BigDecimal inStock = sumInStock(manifest.getSourceId(), manifest.getCategoryCode());
                    if (inStock.compareTo(manifest.getTransferWeight()) < 0) {
                        throw new BizException("在库库存不足，无法转出：当前在库 " + inStock
                                + " 千克，申报转出 " + manifest.getTransferWeight() + " 千克");
                    }
                    // 3) 计划挂靠：未指定则按单位+类别+当年查找，找不到就补建一张
                    Long planId = resolvePlan(manifest);
                    // 4) 建联单（是否跨省按类别名录快照）
                    WasteCategoryPO category = wasteCategoryMapper.selectOne(Wrappers.lambdaQuery(WasteCategoryPO.class)
                            .eq(WasteCategoryPO::getCategoryCode, manifest.getCategoryCode())
                            .last("LIMIT 1"));
                    manifest.setCrossProvince(category != null && category.getCrossProvince() != null
                            ? category.getCrossProvince() : 0);
                    TransferManifestPO manifestPo = TransferManifestPoConverter.toPo(manifest);
                    manifestPo.setId(IdUtil.getSnowflakeNextId());
                    manifestPo.setPlanId(planId);
                    manifestPo.setManifestNo(nextManifestNo());
                    transferManifestMapper.insert(manifestPo);
                    // 5) FIFO 划转在库批次；末批部分划转时拆子批，父批原重量保留
                    allocateFifo(manifest, manifestPo);
                    return TransferManifestPoConverter.toDomain(manifestPo);
                })));
    }

    /** 盘点占用态（COUNTING/PENDING_APPROVAL/APPROVED）存在即拒绝动账。 */
    private void rejectIfFrozen(Long sourceId, String categoryCode, String action) {
        Long blocking = stockCheckMapper.selectCount(Wrappers.lambdaQuery(StockCheckPO.class)
                .eq(StockCheckPO::getSourceId, sourceId)
                .eq(StockCheckPO::getCategoryCode, categoryCode)
                .in(StockCheckPO::getStatus, Arrays.asList(
                        CheckStatus.COUNTING.name(),
                        CheckStatus.PENDING_APPROVAL.name(),
                        CheckStatus.APPROVED.name())));
        if (blocking != null && blocking > 0) {
            throw new BizException("该单位该类别正在盘点中，" + action + "暂停，待盘点调账或作废后再操作");
        }
    }

    /** 在库合计：只认 IN_STOCK 批次（含盘点调整批的正负重量）。 */
    private BigDecimal sumInStock(Long sourceId, String categoryCode) {
        QueryWrapper<WasteStockPO> wrapper = Wrappers.query(WasteStockPO.class);
        wrapper.select("IFNULL(SUM(weight_kg),0) AS weight_kg")
                .eq("source_id", sourceId)
                .eq("category_code", categoryCode)
                .eq("status", BatchStatus.IN_STOCK.name());
        List<Object> rows = wasteStockMapper.selectObjs(wrapper);
        if (rows.isEmpty() || rows.get(0) == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(rows.get(0).toString());
    }

    /** 计划挂靠：指定了校验存在；没指定找当年计划，找不到按申报量补建一张已批复计划。 */
    private Long resolvePlan(TransferManifest manifest) {
        if (manifest.getPlanId() != null) {
            if (transferPlanMapper.selectById(manifest.getPlanId()) == null) {
                throw new BizException("转移计划不存在：" + manifest.getPlanId());
            }
            return manifest.getPlanId();
        }
        int year = Year.now().getValue();
        List<TransferPlanPO> plans = transferPlanMapper.selectList(Wrappers.lambdaQuery(TransferPlanPO.class)
                .eq(TransferPlanPO::getSourceId, manifest.getSourceId())
                .eq(TransferPlanPO::getCategoryCode, manifest.getCategoryCode())
                .eq(TransferPlanPO::getPlanYear, year)
                .orderByDesc(TransferPlanPO::getId)
                .last("LIMIT 1"));
        if (!plans.isEmpty()) {
            return plans.get(0).getId();
        }
        TransferPlanPO plan = new TransferPlanPO();
        plan.setId(IdUtil.getSnowflakeNextId());
        plan.setPlanNo(nextPlanNo());
        plan.setSourceId(manifest.getSourceId());
        plan.setCategoryCode(manifest.getCategoryCode());
        plan.setPlanYear(year);
        plan.setPlannedWeight(manifest.getTransferWeight());
        plan.setApprovedWeight(manifest.getTransferWeight());
        plan.setStatus("APPROVED");
        transferPlanMapper.insert(plan);
        return plan.getId();
    }

    /**
     * FIFO 划转：按入库先后（in_at、id 升序）逐批覆盖申报重量。
     * - 整批覆盖：批次转 TRANSFERRED 并挂联单；
     * - 末批部分覆盖：拆子批——父批保留原重量转 TRANSFERRED，子批带剩余重量留在 IN_STOCK
     *   （parent_batch_id 指向父批），历史批次的原始重量不改写；
     * - 盘点调整批（重量 <= 0）不是实物，不参与划转。
     */
    private void allocateFifo(TransferManifest manifest, TransferManifestPO manifestPo) {
        List<WasteStockPO> batches = wasteStockMapper.selectList(Wrappers.lambdaQuery(WasteStockPO.class)
                .eq(WasteStockPO::getSourceId, manifest.getSourceId())
                .eq(WasteStockPO::getCategoryCode, manifest.getCategoryCode())
                .eq(WasteStockPO::getStatus, BatchStatus.IN_STOCK.name())
                .orderByAsc(WasteStockPO::getInAt)
                .orderByAsc(WasteStockPO::getId));
        BigDecimal remaining = manifest.getTransferWeight();
        for (WasteStockPO batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (batch.getWeightKg() == null || batch.getWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (batch.getWeightKg().compareTo(remaining) <= 0) {
                markTransferred(batch, manifestPo.getId());
                remaining = remaining.subtract(batch.getWeightKg());
            } else {
                // 拆：子批带剩余重量留在库，父批整批（原重量）标记转出
                WasteStockPO child = new WasteStockPO();
                child.setId(IdUtil.getSnowflakeNextId());
                child.setBatchNo(nextBatchNo());
                child.setSourceId(batch.getSourceId());
                child.setCategoryCode(batch.getCategoryCode());
                child.setPackageType(batch.getPackageType());
                child.setWeightKg(batch.getWeightKg().subtract(remaining));
                child.setInAt(batch.getInAt());
                child.setStatus(BatchStatus.IN_STOCK.name());
                child.setParentBatchId(batch.getId());
                wasteStockMapper.insert(child);
                markTransferred(batch, manifestPo.getId());
                remaining = BigDecimal.ZERO;
            }
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // 合计校验已挡过，走到这里说明数据被并发动过；回滚保账
            throw new BizException("在库批次划转不足，转出失败");
        }
    }

    private void markTransferred(WasteStockPO batch, Long manifestId) {
        WasteStockPO upd = new WasteStockPO();
        upd.setId(batch.getId());
        upd.setStatus(BatchStatus.TRANSFERRED.name());
        upd.setManifestId(manifestId);
        wasteStockMapper.updateById(upd);
    }

    private String nextBatchNo() {
        for (int i = 0; i < NO_RETRY; i++) {
            String no = docNoGenerator.next("WB", this::maxBatchNoOfYear);
            if (wasteStockMapper.selectCount(Wrappers.lambdaQuery(WasteStockPO.class)
                    .eq(WasteStockPO::getBatchNo, no)) == 0) {
                return no;
            }
        }
        throw new BizException("批次编号分配冲突，请重试");
    }

    private String nextManifestNo() {
        for (int i = 0; i < NO_RETRY; i++) {
            String no = docNoGenerator.next("EM", this::maxManifestNoOfYear);
            if (transferManifestMapper.selectCount(Wrappers.lambdaQuery(TransferManifestPO.class)
                    .eq(TransferManifestPO::getManifestNo, no)) == 0) {
                return no;
            }
        }
        throw new BizException("联单编号分配冲突，请重试");
    }

    private String nextPlanNo() {
        for (int i = 0; i < NO_RETRY; i++) {
            String no = docNoGenerator.next("TP", this::maxPlanNoOfYear);
            if (transferPlanMapper.selectCount(Wrappers.lambdaQuery(TransferPlanPO.class)
                    .eq(TransferPlanPO::getPlanNo, no)) == 0) {
                return no;
            }
        }
        throw new BizException("计划编号分配冲突，请重试");
    }

    /** 查当年最大编号；序号可能超 4 位，按长度+字典序排，保证取到真最大。 */
    private String maxBatchNoOfYear() {
        List<WasteStockPO> rows = wasteStockMapper.selectList(Wrappers.lambdaQuery(WasteStockPO.class)
                .select(WasteStockPO::getBatchNo)
                .likeRight(WasteStockPO::getBatchNo, docNoGenerator.yearPrefix("WB"))
                .last("ORDER BY LENGTH(batch_no) DESC, batch_no DESC LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0).getBatchNo();
    }

    private String maxManifestNoOfYear() {
        List<TransferManifestPO> rows = transferManifestMapper.selectList(Wrappers.lambdaQuery(TransferManifestPO.class)
                .select(TransferManifestPO::getManifestNo)
                .likeRight(TransferManifestPO::getManifestNo, docNoGenerator.yearPrefix("EM"))
                .last("ORDER BY LENGTH(manifest_no) DESC, manifest_no DESC LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0).getManifestNo();
    }

    private String maxPlanNoOfYear() {
        List<TransferPlanPO> rows = transferPlanMapper.selectList(Wrappers.lambdaQuery(TransferPlanPO.class)
                .select(TransferPlanPO::getPlanNo)
                .likeRight(TransferPlanPO::getPlanNo, docNoGenerator.yearPrefix("TP"))
                .last("ORDER BY LENGTH(plan_no) DESC, plan_no DESC LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0).getPlanNo();
    }

    /** 阻塞 DB 调用 → 响应式链路的桥接器（与 DemoItemRepositoryImpl 同一约定）。 */
    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
