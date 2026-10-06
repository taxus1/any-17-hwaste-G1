package com.somepro.domain.hwaste.model;

/**
 * 入库批次状态（纯领域枚举）。账面在库重量 = IN_STOCK 批次重量加总。
 */
public enum StockStatus {

    /** 在库。 */
    IN_STOCK,
    /** 已转出。 */
    TRANSFERRED,
    /** 已处置。 */
    DISPOSED,
    /** 已作废（如拆分后的父批）。 */
    VOID
}
