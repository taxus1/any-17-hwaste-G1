package com.somepro.domain.hwaste.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存调账流水（纯领域实体）。
 *
 * 每调一次账留一条：盘盈记正数、盘亏记负数，缘由与时刻一并写上。
 * 同一张盘点单只该有一条流水（库表 uk_adjust_check 唯一约束兜底）。
 */
@Getter
@Setter
public class StockAdjust extends BaseEntity {

    private Long id;

    /** 调账流水号，全局唯一，形如 AJ-2026-0001（由仓储层分配）。 */
    private String adjustNo;

    private Long checkId;

    private Long sourceId;

    private String categoryCode;

    /** 调账重量：正为盘盈、负为盘亏。 */
    private BigDecimal adjustWeight;

    private String reason;

    private LocalDateTime adjustedAt;

    /** 工厂方法：按盘点单差异生成调账流水（盘盈正、盘亏负）。 */
    public static StockAdjust of(StockCheck check, String reason) {
        if (check.getDiffWeight() == null) {
            throw new BizException("尚未填实盘，不能调账");
        }
        StockAdjust adjust = new StockAdjust();
        adjust.setCheckId(check.getId());
        adjust.setSourceId(check.getSourceId());
        adjust.setCategoryCode(check.getCategoryCode());
        adjust.setAdjustWeight(check.getDiffWeight());
        adjust.setReason((reason == null || reason.isBlank())
                ? "盘点调账 " + check.getCheckNo()
                : reason.trim());
        adjust.setAdjustedAt(LocalDateTime.now());
        return adjust;
    }
}
