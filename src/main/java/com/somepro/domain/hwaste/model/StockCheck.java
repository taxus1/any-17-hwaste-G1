package com.somepro.domain.hwaste.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 危废库存盘点单（聚合根，纯领域，无框架注解）。
 *
 * 业务规则集中在这里：
 * - 一张单盯一家产废单位 + 一类危废 + 一个月份；立单时账面数由仓储层按在库批次合计快照进来。
 * - 状态机：DRAFT → COUNTING →（NORMAL 直接调账 / MAJOR、CRITICAL → PENDING_APPROVAL → APPROVED）
 *   → ADJUSTED；PENDING_APPROVAL 可驳回为 REJECTED；未终态可作废为 CANCELLED。
 * - 差异 = 实盘 - 账面；差异比例 = |差异| / 账面（账面为 0 时记 0）。
 * - 级别：比例 ≤ 1% 正常；1% < 比例 ≤ 5% 重大；> 5% 严重。
 */
@Getter
@Setter
public class StockCheck extends BaseEntity {

    /** 差异比例阈值：不超过 1% 为正常。 */
    public static final BigDecimal RATIO_NORMAL_LIMIT = new BigDecimal("0.01");
    /** 差异比例阈值：不超过 5%（含）为重大，超过为严重。 */
    public static final BigDecimal RATIO_MAJOR_LIMIT = new BigDecimal("0.05");

    private Long id;

    /** 盘点单编号，全局唯一，形如 CK-2026-0001（由仓储层分配）。 */
    private String checkNo;

    private Long sourceId;

    private String categoryCode;

    /** 盘点期间，形如 2026-08。 */
    private String checkPeriod;

    /** 账面在库重量：立单时按在库批次合计快照。 */
    private BigDecimal bookWeight;

    private BigDecimal countedWeight;

    /** 差异重量 = 实盘 - 账面。 */
    private BigDecimal diffWeight;

    /** 差异比例 = |差异| / 账面，账面为 0 时记 0。 */
    private BigDecimal diffRatio;

    private CheckLevel checkLevel;

    private CheckStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String approver;

    private LocalDateTime approvedAt;

    private String rejectReason;

    /** 工厂方法：立单（落草稿）。bookWeight 由仓储层按在库批次合计取数后传入。 */
    public static StockCheck create(Long sourceId, String categoryCode, String checkPeriod, BigDecimal bookWeight) {
        if (sourceId == null) {
            throw new BizException("产废单位不能为空");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new BizException("危废类别不能为空");
        }
        if (checkPeriod == null || !checkPeriod.trim().matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new BizException("盘点期间格式应为 yyyy-MM，如 2026-08");
        }
        StockCheck check = new StockCheck();
        check.setSourceId(sourceId);
        check.setCategoryCode(categoryCode.trim());
        check.setCheckPeriod(checkPeriod.trim());
        check.setBookWeight(bookWeight == null ? BigDecimal.ZERO : bookWeight);
        check.setStatus(CheckStatus.DRAFT);
        return check;
    }

    /** 开工：草稿 → 盘点中，记开工时刻。 */
    public void start() {
        require(status == CheckStatus.DRAFT, "只有草稿状态的盘点单才能开工");
        this.status = CheckStatus.COUNTING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 填实盘：算差异、定级别、记完成时刻。
     * 正常档留在盘点中（可直接调账）；重大/严重转待审批。
     */
    public void count(BigDecimal counted) {
        require(status == CheckStatus.COUNTING, "只有盘点中的单子才能填实盘");
        if (counted == null || counted.signum() < 0) {
            throw new BizException("实盘重量不能为负");
        }
        this.countedWeight = counted;
        this.diffWeight = counted.subtract(bookWeight);
        this.diffRatio = bookWeight.signum() == 0
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : diffWeight.abs().divide(bookWeight, 4, RoundingMode.HALF_UP);
        this.checkLevel = classify(diffRatio);
        this.finishedAt = LocalDateTime.now();
        if (this.checkLevel != CheckLevel.NORMAL) {
            this.status = CheckStatus.PENDING_APPROVAL;
        }
    }

    /** 批准：待审批 → 已批准。 */
    public void approve(String approver) {
        require(status == CheckStatus.PENDING_APPROVAL, "只有待审批的盘点单才能批准");
        this.status = CheckStatus.APPROVED;
        if (approver != null && !approver.isBlank()) {
            this.approver = approver.trim();
        }
        this.approvedAt = LocalDateTime.now();
    }

    /** 驳回：待审批 → 已驳回（终态），必须写明理由。 */
    public void reject(String reason) {
        require(status == CheckStatus.PENDING_APPROVAL, "只有待审批的盘点单才能驳回");
        if (reason == null || reason.isBlank()) {
            throw new BizException("驳回必须写明理由");
        }
        this.status = CheckStatus.REJECTED;
        this.rejectReason = reason.trim();
    }

    /** 作废：未终态（未调账、未驳回、未作废）才能作废；作废是终态。 */
    public void cancel() {
        require(status != CheckStatus.ADJUSTED && status != CheckStatus.REJECTED
                        && status != CheckStatus.CANCELLED,
                "已调账、已驳回或已作废的盘点单不能再作废");
        this.status = CheckStatus.CANCELLED;
    }

    /**
     * 调账资格校验并把单子置为已调账。
     * 正常档盘点中可直接调；重大/严重必须先批准（已批准态）才能调。
     */
    public void adjust() {
        boolean normalDirect = status == CheckStatus.COUNTING && checkLevel == CheckLevel.NORMAL;
        require(normalDirect || status == CheckStatus.APPROVED,
                "当前状态不允许调账：正常档需盘点中直接调，重大/严重需先批准");
        this.status = CheckStatus.ADJUSTED;
    }

    private static CheckLevel classify(BigDecimal ratio) {
        if (ratio.compareTo(RATIO_NORMAL_LIMIT) <= 0) {
            return CheckLevel.NORMAL;
        }
        if (ratio.compareTo(RATIO_MAJOR_LIMIT) <= 0) {
            return CheckLevel.MAJOR;
        }
        return CheckLevel.CRITICAL;
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new BizException(message);
        }
    }
}
