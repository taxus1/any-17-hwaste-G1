package com.somepro.interfaces.rest.inventory.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转移联单对外返回对象（VO，用户接口层）—— 不可变 record。
 */
public record ManifestVO(
        Long id,
        String manifestNo,
        Long planId,
        Long sourceId,
        Long unitId,
        String categoryCode,
        String transporter,
        BigDecimal transferWeight,
        Integer crossProvince,
        String status,
        LocalDateTime createTime) implements Serializable {
}
