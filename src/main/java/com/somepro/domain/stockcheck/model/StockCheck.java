package com.somepro.domain.stockcheck.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 危废库存盘点单（聚合根，领域层）。
 *
 * 一张单锁定「一家产废单位 + 一类危废 + 一个月份」：
 * - 立单时由应用层取当前在库批次合计作为账面重量快照（bookWeight），本类不自己编数；
 * - 状态流转全部走领域行为（start/finishCount/approve/reject/markAdjusted/cancel），
 *   非法流转在这里直接抛业务异常，仓储层再用「前置状态条件更新」兜底并发；
 * - 差异 = 实盘 - 账面；差异比例 = |差异| / 账面（账面为 0 时记 0），按 4 位小数落库，
 *   分级判定就用这个落库值，保证「看到的比例」与「落库的级别」自洽。
 */
@Getter
@Setter
public class StockCheck extends BaseEntity {

    /** 盘点期间格式：形如 2026-08。 */
    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private Long id;

    /** 盘点单编号（CK-2026-0001），立单落库时由仓储层统一取号。 */
    private String checkNo;

    private Long sourceId;

    private String categoryCode;

    /** 盘点期间，形如 2026-08。 */
    private String checkPeriod;

    /** 账面在库重量（立单时在库批次合计快照）。 */
    private BigDecimal bookWeight = BigDecimal.ZERO;

    /** 实盘重量。 */
    private BigDecimal countedWeight;

    /** 差异重量（实盘 - 账面）。 */
    private BigDecimal diffWeight;

    /** 差异比例（|差异| / 账面，账面为 0 时记 0，4 位小数）。 */
    private BigDecimal diffRatio;

    private CheckLevel checkLevel;

    private CheckStatus status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String approver;

    private LocalDateTime approvedAt;

    private String rejectReason;

    /** 工厂方法：立单，落 DRAFT。账面重量必须是调用方从在库批次实时合计来的。 */
    public static StockCheck create(Long sourceId, String categoryCode, String checkPeriod, BigDecimal bookWeight) {
        if (sourceId == null) {
            throw new BizException("产废单位不能为空");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new BizException("危废类别不能为空");
        }
        if (checkPeriod == null || !PERIOD_PATTERN.matcher(checkPeriod).matches()) {
            throw new BizException("盘点期间格式不正确，应形如 2026-08");
        }
        if (bookWeight == null || bookWeight.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("账面重量不能为负");
        }
        StockCheck check = new StockCheck();
        check.setSourceId(sourceId);
        check.setCategoryCode(categoryCode.trim());
        check.setCheckPeriod(checkPeriod);
        check.setBookWeight(bookWeight);
        check.setStatus(CheckStatus.DRAFT);
        return check;
    }

    /** 开工：DRAFT → COUNTING，记开工时刻。 */
    public void start() {
        requireStatus(CheckStatus.DRAFT, "只有草稿状态的盘点单才能开始盘点");
        this.status = CheckStatus.COUNTING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 填实盘：COUNTING → APPROVED（正常档免审）或 PENDING_APPROVAL（重大/严重）。
     * 差异、比例、级别在这里一次算清并落库。
     */
    public void finishCount(BigDecimal counted) {
        requireStatus(CheckStatus.COUNTING, "只有盘点中的单子才能填写实盘重量");
        if (counted == null || counted.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("实盘重量不能为负");
        }
        this.countedWeight = counted;
        this.diffWeight = counted.subtract(this.bookWeight);
        this.diffRatio = computeDiffRatio(this.diffWeight, this.bookWeight);
        this.checkLevel = CheckLevel.of(this.diffRatio);
        this.finishedAt = LocalDateTime.now();
        // 正常档不必等审批，直接落到可调账的已批准态；重大/严重先送审批
        this.status = (this.checkLevel == CheckLevel.NORMAL) ? CheckStatus.APPROVED : CheckStatus.PENDING_APPROVAL;
    }

    /** 批准：PENDING_APPROVAL → APPROVED。 */
    public void approve(String approver) {
        requireStatus(CheckStatus.PENDING_APPROVAL, "只有待审批的盘点单才能批准");
        this.status = CheckStatus.APPROVED;
        this.approver = approver;
        this.approvedAt = LocalDateTime.now();
    }

    /** 驳回：PENDING_APPROVAL → REJECTED，必须写明理由。 */
    public void reject(String approver, String reason) {
        requireStatus(CheckStatus.PENDING_APPROVAL, "只有待审批的盘点单才能驳回");
        if (reason == null || reason.isBlank()) {
            throw new BizException("驳回必须填写理由");
        }
        this.status = CheckStatus.REJECTED;
        this.approver = approver;
        this.approvedAt = LocalDateTime.now();
        this.rejectReason = reason.trim();
    }

    /** 调账完成：APPROVED → ADJUSTED。 */
    public void markAdjusted() {
        requireStatus(CheckStatus.APPROVED, "只有已批准的盘点单才能调账");
        this.status = CheckStatus.ADJUSTED;
    }

    /** 作废：非终态 → CANCELLED；已调账/已驳回/已作废的单子不再动。 */
    public void cancel() {
        if (this.status == null) {
            throw new BizException("盘点单状态缺失，无法作废");
        }
        if (this.status.isTerminal()) {
            throw new BizException("已调账、已驳回或已作废的盘点单不能再作废");
        }
        this.status = CheckStatus.CANCELLED;
    }

    /** 差异是否为 0（调账时决定是否要动库存批次）。 */
    public boolean hasDiff() {
        return this.diffWeight != null && this.diffWeight.compareTo(BigDecimal.ZERO) != 0;
    }

    private void requireStatus(CheckStatus expected, String message) {
        if (this.status != expected) {
            throw new BizException(message + "（当前状态：" + (this.status == null ? "无" : this.status.name()) + "）");
        }
    }

    /** 差异比例 = |差异| / 账面，账面为 0 时记 0；保留 4 位小数（对应 diff_ratio 的 DECIMAL(8,4)）。 */
    private static BigDecimal computeDiffRatio(BigDecimal diff, BigDecimal book) {
        if (book.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return diff.abs().divide(book, 4, RoundingMode.HALF_UP);
    }
}
