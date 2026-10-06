package com.somepro.interfaces.rest.stockcheck.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 对账结果对外返回对象（VO，用户接口层）—— 不可变 record。
 * balanceWeight 为当前在库批次实时合计（结存），调完账后应等于实盘 countedWeight。
 */
public record ReconcileVO(
        String checkNo,
        Long sourceId,
        String categoryCode,
        String checkPeriod,
        BigDecimal bookWeight,
        BigDecimal countedWeight,
        BigDecimal diffWeight,
        BigDecimal adjustWeight,
        BigDecimal balanceWeight,
        String status) implements Serializable {
}
