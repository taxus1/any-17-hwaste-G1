package com.somepro.application.stockcheck;

import com.somepro.common.exception.BizException;
import com.somepro.domain.inventory.model.WasteStock;
import com.somepro.domain.inventory.repository.InventoryRepository;
import com.somepro.domain.stockcheck.model.CheckStatus;
import com.somepro.domain.stockcheck.model.ReconcileResult;
import com.somepro.domain.stockcheck.model.StockAdjust;
import com.somepro.domain.stockcheck.model.StockCheck;
import com.somepro.domain.stockcheck.model.CheckLevel;
import com.somepro.domain.stockcheck.repository.StockAdjustRepository;
import com.somepro.domain.stockcheck.repository.StockCheckRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 盘点应用服务（应用层）：编排「立单 → 开工 → 填实盘 → 审批 → 调账 / 作废 → 对账」整条用例。
 *
 * 业务规则（状态机、差异分级）在领域对象 StockCheck 里；这里只做：
 * 取数（账面快照、结存）、调用领域行为、委托仓储落库。
 */
@Service
public class StockCheckAppService {

    private final StockCheckRepository stockCheckRepository;
    private final StockAdjustRepository stockAdjustRepository;
    private final InventoryRepository inventoryRepository;

    public StockCheckAppService(StockCheckRepository stockCheckRepository,
                                StockAdjustRepository stockAdjustRepository,
                                InventoryRepository inventoryRepository) {
        this.stockCheckRepository = stockCheckRepository;
        this.stockAdjustRepository = stockAdjustRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * 立单：账面重量取该单位+类别当前在库批次合计（与入库批次同口径），
     * 同组合未作废盘点单只准一张（仓储层临界区查重）。
     */
    public Mono<StockCheck> create(Long sourceId, String categoryCode, String checkPeriod) {
        return Mono.zip(
                        inventoryRepository.existsSource(sourceId),
                        inventoryRepository.existsCategory(categoryCode),
                        inventoryRepository.sumInStockWeight(sourceId, categoryCode))
                .flatMap(t -> {
                    if (!t.getT1()) {
                        return Mono.error(new BizException("产废单位不存在：" + sourceId));
                    }
                    if (!t.getT2()) {
                        return Mono.error(new BizException("危废类别不存在：" + categoryCode));
                    }
                    StockCheck check = StockCheck.create(sourceId, categoryCode, checkPeriod, t.getT3());
                    return stockCheckRepository.createIfAbsent(check);
                });
    }

    /** 开工：DRAFT → COUNTING，记开工时刻；此后该单位+类别冻结进出。 */
    public Mono<StockCheck> start(Long id) {
        return findOrThrow(id)
                .map(check -> {
                    check.start();
                    return check;
                })
                .flatMap(check -> transition(check, CheckStatus.DRAFT));
    }

    /** 填实盘：算差异/比例/级别；正常档直接落到可调账态，重大/严重送待审批。 */
    public Mono<StockCheck> count(Long id, BigDecimal countedWeight) {
        return findOrThrow(id)
                .map(check -> {
                    check.finishCount(countedWeight);
                    return check;
                })
                .flatMap(check -> transition(check, CheckStatus.COUNTING));
    }

    /** 批准：PENDING_APPROVAL → APPROVED。 */
    public Mono<StockCheck> approve(Long id, String approver) {
        return findOrThrow(id)
                .map(check -> {
                    check.approve(approver);
                    return check;
                })
                .flatMap(check -> transition(check, CheckStatus.PENDING_APPROVAL));
    }

    /** 驳回：PENDING_APPROVAL → REJECTED，理由必填。 */
    public Mono<StockCheck> reject(Long id, String approver, String reason) {
        return findOrThrow(id)
                .map(check -> {
                    check.reject(approver, reason);
                    return check;
                })
                .flatMap(check -> transition(check, CheckStatus.PENDING_APPROVAL));
    }

    /**
     * 调账：把账拉平——调完该单位+类别在库合计等于实盘。
     * 同一事务内：插调账流水（盘盈正、盘亏负）+ 插盘点调整批次（差异为 0 时不插）
     * + 盘点单 APPROVED → ADJUSTED。同一张单只出一条流水（uk_adjust_check 兜底）。
     */
    public Mono<StockAdjust> adjust(Long id, String reason) {
        return findOrThrow(id)
                .map(check -> {
                    check.markAdjusted();
                    return check;
                })
                .flatMap(check -> {
                    StockAdjust adjust = StockAdjust.of(check, reason);
                    WasteStock adjustBatch = check.hasDiff()
                            ? WasteStock.adjustment(check.getSourceId(), check.getCategoryCode(), check.getDiffWeight())
                            : null;
                    return stockCheckRepository.adjustInTransaction(check, adjust, adjustBatch);
                });
    }

    /** 作废：非终态 → CANCELLED，释放该单位+类别的进出冻结。 */
    public Mono<StockCheck> cancel(Long id) {
        return findOrThrow(id)
                .map(check -> {
                    check.cancel();
                    return check;
                })
                // 作废的前置状态不唯一：expected 传 null，仓储层按「非终态集合」做条件更新
                .flatMap(check -> transition(check, null));
    }

    /** 详情。 */
    public Mono<StockCheck> detail(Long id) {
        return findOrThrow(id);
    }

    /** 分页查询：单位/类别/月份/级别/状态都可挑，啥都不挑就分页列全。 */
    public Mono<PageResult<StockCheck>> page(int pageNum, int pageSize,
                                             Long sourceId, String categoryCode, String checkPeriod,
                                             String checkLevel, String status) {
        CheckLevel level = parseEnum(CheckLevel.class, checkLevel, "差异级别");
        CheckStatus st = parseEnum(CheckStatus.class, status, "盘点单状态");
        return stockCheckRepository.page(pageNum, pageSize, sourceId, categoryCode, checkPeriod, level, st);
    }

    /**
     * 对账：给单位+类别+月份，回账面、实盘、差异、调账额与结存。
     * 结存取当前在库批次实时合计，与入库批次那边同口径；调完账后结存应等于实盘。
     */
    public Mono<ReconcileResult> reconcile(Long sourceId, String categoryCode, String checkPeriod) {
        return stockCheckRepository.findActiveByCombo(sourceId, categoryCode, checkPeriod)
                .switchIfEmpty(Mono.error(new BizException("该单位该类别该月份没有未作废的盘点单")))
                .flatMap(check -> Mono.zip(
                                inventoryRepository.sumInStockWeight(sourceId, categoryCode),
                                stockAdjustRepository.findByCheckId(check.getId())
                                        .map(adjust -> Optional.ofNullable(adjust.getAdjustWeight()))
                                        .defaultIfEmpty(Optional.empty()))
                        .map(t -> new ReconcileResult(
                                check.getCheckNo(),
                                check.getSourceId(),
                                check.getCategoryCode(),
                                check.getCheckPeriod(),
                                check.getBookWeight(),
                                check.getCountedWeight(),
                                check.getDiffWeight(),
                                t.getT2().orElse(null),
                                t.getT1(),
                                check.getStatus())));
    }

    private Mono<StockCheck> findOrThrow(Long id) {
        return stockCheckRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("盘点单不存在：" + id)));
    }

    /** 状态机流转统一回写：前置状态条件更新，并发下只有一个请求能推进。 */
    private Mono<StockCheck> transition(StockCheck check, CheckStatus expected) {
        return stockCheckRepository.updateWithExpectedStatus(check, expected)
                .flatMap(ok -> ok
                        ? Mono.just(check)
                        : Mono.error(new BizException("盘点单状态已被其他操作改变，请刷新后重试")));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(label + "不合法：" + value);
        }
    }
}
