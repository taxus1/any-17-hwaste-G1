package com.somepro.domain.hwaste.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 对账视图（领域值对象，不可变 record）：给单位 + 类别 + 月份，回一份账。
 *
 * 账面 / 实盘 / 差异取自该期间最新一份未作废盘点单；
 * 结存为当前在库批次实际合计（调完账后应等于实盘）。
 */
public record StockReconciliation(
        String checkNo,
        Long sourceId,
        String categoryCode,
        String checkPeriod,
        CheckStatus status,
        BigDecimal bookWeight,
        BigDecimal countedWeight,
        BigDecimal diffWeight,
        BigDecimal balanceWeight) implements Serializable {
}
