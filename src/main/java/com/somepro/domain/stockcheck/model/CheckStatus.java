package com.somepro.domain.stockcheck.model;

/**
 * 盘点单状态机（领域层）。
 *
 * 流转路径：
 *   DRAFT --开始--> COUNTING --填实盘--> APPROVED（正常档免审）或 PENDING_APPROVAL（重大/严重）
 *   PENDING_APPROVAL --批准--> APPROVED --调账--> ADJUSTED
 *   PENDING_APPROVAL --驳回--> REJECTED
 *   任意非终态 --作废--> CANCELLED
 * 终态：ADJUSTED / REJECTED / CANCELLED，不再允许任何流转。
 */
public enum CheckStatus {

    /** 草稿：已立单、已取账面快照，尚未开工（不冻结库存）。 */
    DRAFT,

    /** 盘点中：已开工，该单位+类别的入库与联单转出冻结。 */
    COUNTING,

    /** 待审批：重大/严重差异，等待审批。 */
    PENDING_APPROVAL,

    /** 已批准：可以调账（正常档填完实盘直接落到此态）。 */
    APPROVED,

    /** 已驳回：终态。 */
    REJECTED,

    /** 已调账：终态。 */
    ADJUSTED,

    /** 已作废：终态。 */
    CANCELLED;

    /** 是否终态（已调账/已驳回/已作废），终态后不得再流转。 */
    public boolean isTerminal() {
        return this == ADJUSTED || this == REJECTED || this == CANCELLED;
    }

    /** 是否处于「盘点占用」态：这些状态下该单位+类别的入库与转出必须冻结。 */
    public boolean isBlocking() {
        return this == COUNTING || this == PENDING_APPROVAL || this == APPROVED;
    }
}
