package com.somepro.interfaces.rest.stockcheck.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 调账流水对外返回对象（VO，用户接口层）—— 不可变 record。
 */
public record StockAdjustVO(
        Long id,
        String adjustNo,
        Long checkId,
        Long sourceId,
        String categoryCode,
        BigDecimal adjustWeight,
        String reason,
        LocalDateTime adjustedAt,
        LocalDateTime createTime) implements Serializable {
}
