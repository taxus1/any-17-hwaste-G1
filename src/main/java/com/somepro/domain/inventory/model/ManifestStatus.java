package com.somepro.domain.inventory.model;

/**
 * 电子转移联单状态（领域层）。本模块只负责创建（SUBMITTED），其余状态列全备查。
 */
public enum ManifestStatus {

    /** 已提交。 */
    SUBMITTED,

    /** 已审批。 */
    APPROVED,

    /** 已退回。 */
    REJECTED,

    /** 运输中。 */
    IN_TRANSIT,

    /** 已签收。 */
    RECEIVED,

    /** 已处置。 */
    DISPOSED,

    /** 已作废。 */
    VOID
}
