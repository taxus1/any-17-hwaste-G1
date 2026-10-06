package com.somepro.interfaces.rest.hwaste.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 联单转出结果对外返回对象（VO，用户接口层）—— 不可变 record。
 */
public record TransferResultVO(
        Long sourceId,
        String categoryCode,
        BigDecimal transferredWeight) implements Serializable {
}
