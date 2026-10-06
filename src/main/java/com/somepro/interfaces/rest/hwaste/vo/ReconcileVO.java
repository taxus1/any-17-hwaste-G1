package com.somepro.interfaces.rest.hwaste.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 对账结果对外返回对象（VO，用户接口层）—— 不可变 record。
 * 账面 / 实盘 / 差异来自盘点单，结存为当前在库批次实际合计。
 */
public record ReconcileVO(
        String checkNo,
        Long sourceId,
        String categoryCode,
        String checkPeriod,
        String status,
        BigDecimal bookWeight,
        BigDecimal countedWeight,
        BigDecimal diffWeight,
        BigDecimal balanceWeight) implements Serializable {
}
