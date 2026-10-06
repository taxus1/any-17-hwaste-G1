package com.somepro.infrastructure.persistence.hwaste;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.StockStatus;
import com.somepro.domain.hwaste.model.WasteStock;
import com.somepro.domain.hwaste.repository.WasteStockRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.base.BaseBlockingRepository;
import com.somepro.infrastructure.persistence.hwaste.converter.WasteStockPoConverter;
import com.somepro.infrastructure.persistence.hwaste.po.WasteStockPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 入库批次仓储适配器（基础设施层）。
 *
 * 入库与转出共用一把 WB 锁：批次号「取号 + 落库」串行，转出整段 FIFO 消化也串行，
 * 避免并发转出把同一批库存消化两遍。
 */
@Repository
public class WasteStockRepositoryImpl extends BaseBlockingRepository implements WasteStockRepository {

    private final WasteStockMapper wasteStockMapper;
    private final BizNoService bizNoService;
    private final TransactionTemplate txTemplate;

    public WasteStockRepositoryImpl(WasteStockMapper wasteStockMapper, BizNoService bizNoService,
                                    PlatformTransactionManager transactionManager) {
        this.wasteStockMapper = wasteStockMapper;
        this.bizNoService = bizNoService;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<WasteStock> inbound(WasteStock stock) {
        return blocking(() -> bizNoService.inLock("WB", () -> {
            WasteStockPO po = WasteStockPoConverter.toPo(stock);
            po.setId(IdUtil.getSnowflakeNextId());
            po.setBatchNo(bizNoService.nextBatchNo());
            wasteStockMapper.insert(po);
            return WasteStockPoConverter.toDomain(po);
        }));
    }

    @Override
    public Mono<BigDecimal> sumInStock(Long sourceId, String categoryCode) {
        return blocking(() -> wasteStockMapper.sumInStockWeight(sourceId, categoryCode));
    }

    @Override
    public Mono<BigDecimal> transferOut(Long sourceId, String categoryCode, BigDecimal weightKg) {
        return blocking(() -> bizNoService.inLock("WB", () -> txTemplate.execute(tx -> {
            List<WasteStockPO> batches = wasteStockMapper.selectList(Wrappers.<WasteStockPO>lambdaQuery()
                    .eq(WasteStockPO::getSourceId, sourceId)
                    .eq(WasteStockPO::getCategoryCode, categoryCode)
                    .eq(WasteStockPO::getStatus, StockStatus.IN_STOCK.name())
                    .orderByAsc(WasteStockPO::getInAt)
                    .orderByAsc(WasteStockPO::getId));
            BigDecimal total = batches.stream()
                    .map(WasteStockPO::getWeightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(weightKg) < 0) {
                throw new BizException("在库重量不足，无法转出");
            }
            BigDecimal remaining = weightKg;
            for (WasteStockPO batch : batches) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal weight = batch.getWeightKg();
                if (weight.compareTo(remaining) <= 0) {
                    // 整批转出
                    remaining = remaining.subtract(weight);
                    WasteStockPO update = new WasteStockPO();
                    update.setId(batch.getId());
                    update.setStatus(StockStatus.TRANSFERRED.name());
                    wasteStockMapper.updateById(update);
                } else {
                    // 部分转出：父批作废，拆出「转出部分」与「留存部分」两个子批，父批原记录保留
                    insertChild(batch, remaining, StockStatus.TRANSFERRED);
                    insertChild(batch, weight.subtract(remaining), StockStatus.IN_STOCK);
                    WasteStockPO update = new WasteStockPO();
                    update.setId(batch.getId());
                    update.setStatus(StockStatus.VOID.name());
                    wasteStockMapper.updateById(update);
                    remaining = BigDecimal.ZERO;
                }
            }
            return weightKg;
        })));
    }

    @Override
    public Mono<PageResult<WasteStock>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                             String status) {
        return this.<PageResult<WasteStock>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<WasteStockPO> wrapper = Wrappers.<WasteStockPO>lambdaQuery()
                        .eq(sourceId != null, WasteStockPO::getSourceId, sourceId)
                        .eq(categoryCode != null && !categoryCode.isBlank(),
                                WasteStockPO::getCategoryCode, categoryCode)
                        .eq(status != null && !status.isBlank(), WasteStockPO::getStatus, status)
                        .orderByDesc(WasteStockPO::getId);
                List<WasteStockPO> rows = wasteStockMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<WasteStock> content = rows.stream()
                        .map(WasteStockPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    /** 拆分子批：继承父批的单位 / 类别 / 包装 / 入库时刻，parent_batch_id 指回父批。 */
    private void insertChild(WasteStockPO parent, BigDecimal weight, StockStatus status) {
        WasteStockPO child = new WasteStockPO();
        child.setId(IdUtil.getSnowflakeNextId());
        child.setBatchNo(bizNoService.nextBatchNo());
        child.setSourceId(parent.getSourceId());
        child.setCategoryCode(parent.getCategoryCode());
        child.setPackageType(parent.getPackageType());
        child.setWeightKg(weight);
        child.setInAt(parent.getInAt());
        child.setStatus(status.name());
        child.setParentBatchId(parent.getId());
        wasteStockMapper.insert(child);
    }
}
