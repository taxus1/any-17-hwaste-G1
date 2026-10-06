package com.somepro.domain.stockcheck.model;

import java.math.BigDecimal;

/**
 * 对账结果（领域值对象，不可变 record）。
 *
 * 给「单位 + 类别 + 月份」回一份账：
 * - bookWeight：账面（立单时在库批次合计快照）
 * - countedWeight：实盘
 * - diffWeight：差异（实盘 - 账面）
 * - adjustWeight：调账流水重量（未调账为 null）
 * - balanceWeight：结存——当前在库批次的实时合计；调完账后它应等于实盘
 */
public record ReconcileResult(
        String checkNo,
        Long sourceId,
        String categoryCode,
        String checkPeriod,
        BigDecimal bookWeight,
        BigDecimal countedWeight,
        BigDecimal diffWeight,
        BigDecimal adjustWeight,
        BigDecimal balanceWeight,
        CheckStatus status) {
}
