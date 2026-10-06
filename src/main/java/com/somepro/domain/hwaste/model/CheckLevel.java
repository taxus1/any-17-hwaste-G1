package com.somepro.domain.hwaste.model;

/**
 * 盘点差异级别（纯领域枚举）。
 * NORMAL 正常（|差异|/账面 ≤ 1%）/ MAJOR 重大（1% < 比例 ≤ 5%）/ CRITICAL 严重（> 5%）。
 */
public enum CheckLevel {

    NORMAL,
    MAJOR,
    CRITICAL
}
