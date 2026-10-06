package com.somepro.infrastructure.persistence.hwaste;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.CheckStatus;
import com.somepro.domain.hwaste.model.StockAdjust;
import com.somepro.domain.hwaste.model.StockCheck;
import com.somepro.domain.hwaste.model.StockStatus;
import com.somepro.domain.hwaste.repository.StockAdjustRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.base.BaseBlockingRepository;
import com.somepro.infrastructure.persistence.hwaste.converter.StockAdjustPoConverter;
import com.somepro.infrastructure.persistence.hwaste.po.StockAdjustPO;
import com.somepro.infrastructure.persistence.hwaste.po.StockCheckPO;
import com.somepro.infrastructure.persistence.hwaste.po.WasteStockPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调账流水仓储适配器（基础设施层）。
 *
 * 调账是一个事务：盘点单条件置已调账 → 写调账流水 → 写在库对冲批次。
 * 防重两道闸：状态条件更新（并发下只有一个事务能把单子从 COUNTING/APPROVED 推走），
 * 加上 uk_adjust_check 唯一约束兜底 —— 同一张盘点单只该出一条流水。
 */
@Repository
public class StockAdjustRepositoryImpl extends BaseBlockingRepository implements StockAdjustRepository {

    private final StockAdjustMapper stockAdjustMapper;
    private final StockCheckMapper stockCheckMapper;
    private final WasteStockMapper wasteStockMapper;
    private final BizNoService bizNoService;
    private final TransactionTemplate txTemplate;

    public StockAdjustRepositoryImpl(StockAdjustMapper stockAdjustMapper,
                                     StockCheckMapper stockCheckMapper,
                                     WasteStockMapper wasteStockMapper,
                                     BizNoService bizNoService,
                                     PlatformTransactionManager transactionManager) {
        this.stockAdjustMapper = stockAdjustMapper;
        this.stockCheckMapper = stockCheckMapper;
        this.wasteStockMapper = wasteStockMapper;
        this.bizNoService = bizNoService;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public Mono<StockAdjust> applyAdjustment(StockCheck check, StockAdjust adjust) {
        return blocking(() -> txTemplate.execute(tx -> {
            // 1. 条件更新盘点单状态：只有 COUNTING / APPROVED 能推到 ADJUSTED，
            //    并发重复调账时后到的更新 0 行，整个事务回滚，不会重复记账
            StockCheckPO statusUpdate = new StockCheckPO();
            statusUpdate.setStatus(CheckStatus.ADJUSTED.name());
            int rows = stockCheckMapper.update(statusUpdate, Wrappers.<StockCheckPO>lambdaUpdate()
                    .eq(StockCheckPO::getId, check.getId())
                    .in(StockCheckPO::getStatus, CheckStatus.COUNTING.name(), CheckStatus.APPROVED.name()));
            if (rows == 0) {
                throw new BizException("盘点单已调账或状态已变化，请勿重复调账");
            }
            // 2. 调账流水：一单一条（uk_adjust_check 兜底），盘盈正数、盘亏负数
            StockAdjustPO adjustPo = StockAdjustPoConverter.toPo(adjust);
            adjustPo.setId(IdUtil.getSnowflakeNextId());
            bizNoService.inLock("AJ", () -> {
                adjustPo.setAdjustNo(bizNoService.nextAdjustNo());
                stockAdjustMapper.insert(adjustPo);
                return null;
            });
            // 3. 在库对冲批次：盘盈补正数、盘亏下扣负数；历史批次一行不动，调整另立痕迹
            if (adjust.getAdjustWeight().signum() != 0) {
                bizNoService.inLock("WB", () -> {
                    WasteStockPO offset = new WasteStockPO();
                    offset.setId(IdUtil.getSnowflakeNextId());
                    offset.setBatchNo(bizNoService.nextBatchNo());
                    offset.setSourceId(adjust.getSourceId());
                    offset.setCategoryCode(adjust.getCategoryCode());
                    offset.setPackageType("BULK");
                    offset.setWeightKg(adjust.getAdjustWeight());
                    offset.setInAt(LocalDateTime.now());
                    offset.setStatus(StockStatus.IN_STOCK.name());
                    wasteStockMapper.insert(offset);
                    return null;
                });
            }
            return StockAdjustPoConverter.toDomain(adjustPo);
        }));
    }

    @Override
    public Mono<PageResult<StockAdjust>> page(int pageNum, int pageSize, Long checkId, Long sourceId,
                                              String categoryCode) {
        return this.<PageResult<StockAdjust>>blocking(() -> {
            try {
                PageHelper.startPage(pageNum, pageSize);
                LambdaQueryWrapper<StockAdjustPO> wrapper = Wrappers.<StockAdjustPO>lambdaQuery()
                        .eq(checkId != null, StockAdjustPO::getCheckId, checkId)
                        .eq(sourceId != null, StockAdjustPO::getSourceId, sourceId)
                        .eq(categoryCode != null && !categoryCode.isBlank(),
                                StockAdjustPO::getCategoryCode, categoryCode)
                        .orderByDesc(StockAdjustPO::getId);
                List<StockAdjustPO> rows = stockAdjustMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<StockAdjust> content = rows.stream()
                        .map(StockAdjustPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                PageHelper.clearPage();
            }
        });
    }
}
