package com.somepro.domain.inventory.model;

/**
 * 入库批次状态（领域层）。
 */
public enum BatchStatus {

    /** 在库。 */
    IN_STOCK,

    /** 已转出（关联联单）。 */
    TRANSFERRED,

    /** 已处置。 */
    DISPOSED,

    /** 已作废。 */
    VOID
}
