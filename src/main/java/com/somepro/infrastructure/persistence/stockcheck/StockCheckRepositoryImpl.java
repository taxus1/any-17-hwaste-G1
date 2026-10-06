package com.somepro.infrastructure.persistence.stockcheck;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.inventory.model.WasteStock;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.stockcheck.model.CheckLevel;
import com.somepro.domain.stockcheck.model.CheckStatus;
import com.somepro.domain.stockcheck.model.StockAdjust;
import com.somepro.domain.stockcheck.model.StockCheck;
import com.somepro.domain.stockcheck.repository.StockCheckRepository;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.inventory.WasteStockMapper;
import com.somepro.infrastructure.persistence.inventory.converter.WasteStockPoConverter;
import com.somepro.infrastructure.persistence.inventory.po.WasteStockPO;
import com.somepro.infrastructure.persistence.stockcheck.converter.StockAdjustPoConverter;
import com.somepro.infrastructure.persistence.stockcheck.converter.StockCheckPoConverter;
import com.somepro.infrastructure.persistence.stockcheck.po.StockAdjustPO;
import com.somepro.infrastructure.persistence.stockcheck.po.StockCheckPO;
import com.somepro.infrastructure.persistence.support.ComboLockRegistry;
import com.somepro.infrastructure.persistence.support.DocNoGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 盘点单仓储适配器（基础设施层）。
 *
 * 关键实现约定：
 * - 立单：「单位+类别+月份」组合锁内查重（未作废即挡回）+ 取号插入，并发立单只成一份；
 * - 状态流转：「单位+类别」库存互斥锁内做前置状态条件更新（UPDATE ... WHERE status = 前置态），
 *   既防并发重复推进，又与入库/转出互斥（开工瞬间不会有人正在动账）；
 * - 调账：流水 + 调整批次 + 状态翻转在同一事务；uk_adjust_check 唯一约束兜底，
 *   同一张盘点单只可能落一条流水，手快点两下也会整体回滚；
 * - 编号（CK/AJ/WB）经 DocNoGenerator 取号，撞唯一键重取重插，绝不重号。
 */
@Repository
public class StockCheckRepositoryImpl implements StockCheckRepository {

    /** 取号撞唯一键时的最大重试次数。 */
    private static final int NO_RETRY = 3;

    private final StockCheckMapper stockCheckMapper;
    private final StockAdjustMapper stockAdjustMapper;
    private final WasteStockMapper wasteStockMapper;
    private final DocNoGenerator docNoGenerator;
    private final ComboLockRegistry comboLockRegistry;
    private final TransactionTemplate txTemplate;

    public StockCheckRepositoryImpl(StockCheckMapper stockCheckMapper,
                                    StockAdjustMapper stockAdjustMapper,
                                    WasteStockMapper wasteStockMapper,
                                    DocNoGenerator docNoGenerator,
                                    ComboLockRegistry comboLockRegistry,
                                    PlatformTransactionManager txManager) {
        this.stockCheckMapper = stockCheckMapper;
        this.stockAdjustMapper = stockAdjustMapper;
        this.wasteStockMapper = wasteStockMapper;
        this.docNoGenerator = docNoGenerator;
        this.comboLockRegistry = comboLockRegistry;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public Mono<StockCheck> createIfAbsent(StockCheck check) {
        return blocking(() -> comboLockRegistry.withLock(
                comboLockRegistry.checkCreateKey(check.getSourceId(), check.getCategoryCode(), check.getCheckPeriod()),
                () -> {
                    Long active = stockCheckMapper.selectCount(Wrappers.lambdaQuery(StockCheckPO.class)
                            .eq(StockCheckPO::getSourceId, check.getSourceId())
                            .eq(StockCheckPO::getCategoryCode, check.getCategoryCode())
                            .eq(StockCheckPO::getCheckPeriod, check.getCheckPeriod())
                            .ne(StockCheckPO::getStatus, CheckStatus.CANCELLED.name()));
                    if (active != null && active > 0) {
                        throw new BizException("该单位该类别该月份已存在未作废的盘点单，请勿重复立单");
                    }
                    StockCheckPO po = StockCheckPoConverter.toPo(check);
                    po.setId(IdUtil.getSnowflakeNextId());
                    for (int i = 0; i < NO_RETRY; i++) {
                        po.setCheckNo(docNoGenerator.next("CK", this::maxCheckNoOfYear));
                        try {
                            stockCheckMapper.insert(po);
                            return StockCheckPoConverter.toDomain(po);
                        } catch (DuplicateKeyException e) {
                            // 撞 uk_check_no：重取号重插；序号允许空洞，不允许重复
                            if (i == NO_RETRY - 1) {
                                throw new BizException("盘点单编号分配冲突，请重试");
                            }
                        }
                    }
                    throw new BizException("盘点单编号分配冲突，请重试");
                }));
    }

    @Override
    public Mono<StockCheck> findById(Long id) {
        return blocking(() -> {
            StockCheckPO po = stockCheckMapper.selectById(id);
            return po == null ? null : StockCheckPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<StockCheck> findActiveByCombo(Long sourceId, String categoryCode, String checkPeriod) {
        return blocking(() -> {
            List<StockCheckPO> rows = stockCheckMapper.selectList(Wrappers.lambdaQuery(StockCheckPO.class)
                    .eq(StockCheckPO::getSourceId, sourceId)
                    .eq(StockCheckPO::getCategoryCode, categoryCode)
                    .eq(StockCheckPO::getCheckPeriod, checkPeriod)
                    .ne(StockCheckPO::getStatus, CheckStatus.CANCELLED.name())
                    .orderByDesc(StockCheckPO::getId)
                    .last("LIMIT 1"));
            return rows.isEmpty() ? null : StockCheckPoConverter.toDomain(rows.get(0));
        });
    }

    @Override
    public Mono<Long> countBlocking(Long sourceId, String categoryCode) {
        return blocking(() -> stockCheckMapper.selectCount(blockingQuery(sourceId, categoryCode)));
    }

    @Override
    public Mono<PageResult<StockCheck>> page(int pageNum, int pageSize,
                                             Long sourceId, String categoryCode, String checkPeriod,
                                             CheckLevel checkLevel, CheckStatus status) {
        return this.<PageResult<StockCheck>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<StockCheckPO> wrapper = Wrappers.lambdaQuery(StockCheckPO.class)
                        .eq(sourceId != null, StockCheckPO::getSourceId, sourceId)
                        .eq(categoryCode != null && !categoryCode.isBlank(), StockCheckPO::getCategoryCode, categoryCode)
                        .eq(checkPeriod != null && !checkPeriod.isBlank(), StockCheckPO::getCheckPeriod, checkPeriod)
                        .eq(checkLevel != null, StockCheckPO::getCheckLevel, checkLevel == null ? null : checkLevel.name())
                        .eq(status != null, StockCheckPO::getStatus, status == null ? null : status.name())
                        .orderByDesc(StockCheckPO::getId);
                List<StockCheckPO> rows = stockCheckMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<StockCheck> content = rows.stream()
                        .map(StockCheckPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传参，必须清理，否则污染线程池里的下一次调用
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Boolean> updateWithExpectedStatus(StockCheck check, CheckStatus expected) {
        return blocking(() -> comboLockRegistry.withLock(
                comboLockRegistry.inventoryKey(check.getSourceId(), check.getCategoryCode()),
                () -> {
                    StockCheckPO po = StockCheckPoConverter.toPo(check);
                    LambdaUpdateWrapper<StockCheckPO> wrapper = Wrappers.lambdaUpdate(StockCheckPO.class)
                            .eq(StockCheckPO::getId, check.getId());
                    if (expected != null) {
                        wrapper.eq(StockCheckPO::getStatus, expected.name());
                    } else {
                        // 作废：前置态为任意非终态
                        wrapper.notIn(StockCheckPO::getStatus, Arrays.asList(
                                CheckStatus.ADJUSTED.name(), CheckStatus.REJECTED.name(), CheckStatus.CANCELLED.name()));
                    }
                    // update(po, wrapper)：po 的非 null 字段进 SET，审计 updateBy/updateTime 由 MetaObjectHandler 填充
                    return stockCheckMapper.update(po, wrapper) > 0;
                }));
    }

    @Override
    public Mono<StockAdjust> adjustInTransaction(StockCheck check, StockAdjust adjust, WasteStock adjustBatch) {
        return blocking(() -> comboLockRegistry.withLock(
                comboLockRegistry.inventoryKey(check.getSourceId(), check.getCategoryCode()),
                () -> txTemplate.execute(tx -> {
                    // 1) 调账流水：uk_adjust_check 兜底，同一张单只此一条
                    StockAdjustPO adjustPo = StockAdjustPoConverter.toPo(adjust);
                    adjustPo.setId(IdUtil.getSnowflakeNextId());
                    for (int i = 0; i < NO_RETRY; i++) {
                        adjustPo.setAdjustNo(docNoGenerator.next("AJ", this::maxAdjustNoOfYear));
                        try {
                            stockAdjustMapper.insert(adjustPo);
                            break;
                        } catch (DuplicateKeyException e) {
                            if (isCheckIdConflict(e) || i == NO_RETRY - 1) {
                                // check_id 唯一冲突 = 这张单已调过账；抛业务异常整体回滚
                                throw new BizException("该盘点单已生成调账流水，请勿重复调账");
                            }
                        }
                    }
                    // 2) 盘点调整批次：盘盈正、盘亏负，另立痕迹，历史批次不改写
                    if (adjustBatch != null) {
                        WasteStockPO batchPo = WasteStockPoConverter.toPo(adjustBatch);
                        batchPo.setId(IdUtil.getSnowflakeNextId());
                        for (int i = 0; i < NO_RETRY; i++) {
                            batchPo.setBatchNo(docNoGenerator.next("WB", this::maxBatchNoOfYear));
                            try {
                                wasteStockMapper.insert(batchPo);
                                break;
                            } catch (DuplicateKeyException e) {
                                if (i == NO_RETRY - 1) {
                                    throw new BizException("调整批次编号分配冲突，请重试");
                                }
                            }
                        }
                    }
                    // 3) 盘点单 APPROVED → ADJUSTED（前置状态条件更新，并发双击只中一次）
                    StockCheckPO upd = new StockCheckPO();
                    upd.setStatus(CheckStatus.ADJUSTED.name());
                    int rows = stockCheckMapper.update(upd, Wrappers.lambdaUpdate(StockCheckPO.class)
                            .eq(StockCheckPO::getId, check.getId())
                            .eq(StockCheckPO::getStatus, CheckStatus.APPROVED.name()));
                    if (rows == 0) {
                        throw new BizException("盘点单状态已变化，调账失败");
                    }
                    return StockAdjustPoConverter.toDomain(adjustPo);
                })));
    }

    /** 唯一键冲突是否来自 check_id（uk_adjust_check）：消息里含键名则按「已调账」处理。 */
    private boolean isCheckIdConflict(DuplicateKeyException e) {
        String msg = String.valueOf(e.getMessage());
        return msg.contains("uk_adjust_check") || msg.contains("check_id");
    }

    /** 该单位+类别处于盘点占用态（COUNTING/PENDING_APPROVAL/APPROVED）的查询条件。 */
    private LambdaQueryWrapper<StockCheckPO> blockingQuery(Long sourceId, String categoryCode) {
        return Wrappers.lambdaQuery(StockCheckPO.class)
                .eq(StockCheckPO::getSourceId, sourceId)
                .eq(StockCheckPO::getCategoryCode, categoryCode)
                .in(StockCheckPO::getStatus, Arrays.asList(
                        CheckStatus.COUNTING.name(),
                        CheckStatus.PENDING_APPROVAL.name(),
                        CheckStatus.APPROVED.name()));
    }

    /** 查当年最大编号；序号可能超 4 位，按长度+字典序排，保证取到真最大。 */
    private String maxCheckNoOfYear() {
        List<StockCheckPO> rows = stockCheckMapper.selectList(Wrappers.lambdaQuery(StockCheckPO.class)
                .select(StockCheckPO::getCheckNo)
                .likeRight(StockCheckPO::getCheckNo, docNoGenerator.yearPrefix("CK"))
                .last("ORDER BY LENGTH(check_no) DESC, check_no DESC LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0).getCheckNo();
    }

    private String maxAdjustNoOfYear() {
        List<StockAdjustPO> rows = stockAdjustMapper.selectList(Wrappers.lambdaQuery(StockAdjustPO.class)
                .select(StockAdjustPO::getAdjustNo)
                .likeRight(StockAdjustPO::getAdjustNo, docNoGenerator.yearPrefix("AJ"))
                .last("ORDER BY LENGTH(adjust_no) DESC, adjust_no DESC LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0).getAdjustNo();
    }

    private String maxBatchNoOfYear() {
        List<WasteStockPO> rows = wasteStockMapper.selectList(Wrappers.lambdaQuery(WasteStockPO.class)
                .select(WasteStockPO::getBatchNo)
                .likeRight(WasteStockPO::getBatchNo, docNoGenerator.yearPrefix("WB"))
                .last("ORDER BY LENGTH(batch_no) DESC, batch_no DESC LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0).getBatchNo();
    }

    /**
     * 阻塞 DB 调用 → 响应式链路的桥接器（与 DemoItemRepositoryImpl 同一约定）：
     * 先取 Reactor Context 里的操作人，再切 boundedElastic 执行 JDBC，审计字段靠 AuditContextHolder 传递。
     */
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
