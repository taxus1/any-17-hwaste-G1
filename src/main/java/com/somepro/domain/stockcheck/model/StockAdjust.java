package com.somepro.domain.stockcheck.model;

import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存调账流水（领域层）。
 *
 * 每次调账留一条：盘盈记正数、盘亏记负数，缘由与时刻一并落库。
 * 同一张盘点单只该有一条流水（库表 uk_adjust_check 唯一约束兜底），
 * 它是盘点调整的独立痕迹，与历史批次原有的进出记录分开存。
 */
@Getter
@Setter
public class StockAdjust extends BaseEntity {

    private Long id;

    /** 调账流水号（AJ-2026-0001），落库时由仓储层统一取号。 */
    private String adjustNo;

    /** 来源盘点单 id。 */
    private Long checkId;

    private Long sourceId;

    private String categoryCode;

    /** 调账重量：正为盘盈、负为盘亏。 */
    private BigDecimal adjustWeight;

    /** 调账缘由。 */
    private String reason;

    /** 调账时刻。 */
    private LocalDateTime adjustedAt;

    /** 工厂方法：按盘点单的差异生成一条调账流水（盘盈正、盘亏负、无差异为 0）。 */
    public static StockAdjust of(StockCheck check, String reason) {
        StockAdjust adjust = new StockAdjust();
        adjust.setCheckId(check.getId());
        adjust.setSourceId(check.getSourceId());
        adjust.setCategoryCode(check.getCategoryCode());
        adjust.setAdjustWeight(check.getDiffWeight());
        adjust.setReason(reason);
        adjust.setAdjustedAt(LocalDateTime.now());
        return adjust;
    }
}
