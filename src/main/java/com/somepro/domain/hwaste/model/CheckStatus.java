package com.somepro.domain.hwaste.model;

/**
 * 盘点单状态机（纯领域枚举，落库时存 name() 字符串）。
 *
 * 流转：DRAFT → COUNTING →（正常档直接调账 / 重大严重 → PENDING_APPROVAL → APPROVED）→ ADJUSTED；
 * PENDING_APPROVAL 可驳回 → REJECTED；未终态前可作废 → CANCELLED。
 * ADJUSTED / REJECTED / CANCELLED 为终态，不再流转。
 */
public enum CheckStatus {

    /** 草稿：刚立单，账面数已快照。 */
    DRAFT,
    /** 盘点中：已开工；该单位该类别的新入库与联单转出此时起被冻结。 */
    COUNTING,
    /** 待审批：差异级别为重大/严重，等审批。 */
    PENDING_APPROVAL,
    /** 已批准：可以调账。 */
    APPROVED,
    /** 已驳回：终态。 */
    REJECTED,
    /** 已调账：终态。 */
    ADJUSTED,
    /** 已作废：终态。 */
    CANCELLED
}
