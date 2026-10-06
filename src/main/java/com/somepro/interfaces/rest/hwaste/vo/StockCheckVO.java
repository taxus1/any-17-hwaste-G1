package com.somepro.interfaces.rest.hwaste.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盘点单对外返回对象（VO，用户接口层）—— 不可变 record。
 * 只暴露对外字段，delFlag / createBy / updateBy / updateTime 不进 API 契约。
 */
public record StockCheckVO(
        Long id,
        String checkNo,
        Long sourceId,
        String categoryCode,
        String checkPeriod,
        BigDecimal bookWeight,
        BigDecimal countedWeight,
        BigDecimal diffWeight,
        BigDecimal diffRatio,
        String checkLevel,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String approver,
        LocalDateTime approvedAt,
        String rejectReason,
        LocalDateTime createTime) implements Serializable {
}
