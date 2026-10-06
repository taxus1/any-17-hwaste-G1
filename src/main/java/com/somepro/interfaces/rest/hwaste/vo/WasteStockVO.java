package com.somepro.interfaces.rest.hwaste.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库批次对外返回对象（VO，用户接口层）—— 不可变 record。
 */
public record WasteStockVO(
        Long id,
        String batchNo,
        Long sourceId,
        String categoryCode,
        String packageType,
        BigDecimal weightKg,
        LocalDateTime inAt,
        String status,
        Long manifestId,
        Long parentBatchId,
        LocalDateTime createTime) implements Serializable {
}
