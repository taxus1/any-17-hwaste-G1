package com.somepro.domain.stockcheck.model;

import java.math.BigDecimal;

/**
 * 盘点差异级别（领域层）。
 *
 * 按差异比例（|实盘-账面| / 账面，账面为 0 时比例记 0）分级：
 *   比例 <= 1%        → NORMAL 正常（免审批，可直接调账）
 *   1% < 比例 <= 5%   → MAJOR  重大（需审批）
 *   比例 > 5%         → CRITICAL 严重（需审批）
 */
public enum CheckLevel {

    /** 正常：差异比例不超过 1%。 */
    NORMAL,

    /** 重大：差异比例超过 1% 且不超过 5%。 */
    MAJOR,

    /** 严重：差异比例超过 5%。 */
    CRITICAL;

    /** 正常档阈值：1%。 */
    public static final BigDecimal NORMAL_THRESHOLD = new BigDecimal("0.01");

    /** 重大档阈值：5%。 */
    public static final BigDecimal MAJOR_THRESHOLD = new BigDecimal("0.05");

    /** 按差异比例分级；比例为空（未填实盘）时抛不出——调用方保证先算好比例。 */
    public static CheckLevel of(BigDecimal diffRatio) {
        if (diffRatio.compareTo(NORMAL_THRESHOLD) <= 0) {
            return NORMAL;
        }
        if (diffRatio.compareTo(MAJOR_THRESHOLD) <= 0) {
            return MAJOR;
        }
        return CRITICAL;
    }
}
