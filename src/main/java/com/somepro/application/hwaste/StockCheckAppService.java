package com.somepro.application.hwaste;

import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.StockAdjust;
import com.somepro.domain.hwaste.model.StockCheck;
import com.somepro.domain.hwaste.model.StockReconciliation;
import com.somepro.domain.hwaste.repository.StockAdjustRepository;
import com.somepro.domain.hwaste.repository.StockCheckRepository;
import com.somepro.domain.hwaste.repository.WasteStockRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 盘点用例编排（应用层）：立单 → 开工 → 填实盘 →（审批）→ 调账 / 作废，以及对账。
 *
 * 业务规则在领域对象 {@link StockCheck} 里，这里只做编排：
 * 取账面数、加载聚合、调用领域行为、落库。
 */
@Service
public class StockCheckAppService {

    private final StockCheckRepository stockCheckRepository;
    private final StockAdjustRepository stockAdjustRepository;
    private final WasteStockRepository wasteStockRepository;

    public StockCheckAppService(StockCheckRepository stockCheckRepository,
                                StockAdjustRepository stockAdjustRepository,
                                WasteStockRepository wasteStockRepository) {
        this.stockCheckRepository = stockCheckRepository;
        this.stockAdjustRepository = stockAdjustRepository;
        this.wasteStockRepository = wasteStockRepository;
    }

    /**
     * 立单：先把账面数取出来（该单位该类别在库批次重量合计），再落草稿单。
     * 同单位 + 同类别 + 同月份只准挂一份未作废的单，重复立单由仓储层挡回。
     */
    public Mono<StockCheck> create(Long sourceId, String categoryCode, String checkPeriod) {
        return Mono.defer(() -> {
            // 先走一遍工厂校验入参（单位 / 类别 / 期间格式），账面数占位 0
            StockCheck check = StockCheck.create(sourceId, categoryCode, checkPeriod, BigDecimal.ZERO);
            return wasteStockRepository.sumInStock(check.getSourceId(), check.getCategoryCode())
                    .map(book -> {
                        check.setBookWeight(book);
                        return check;
                    })
                    .flatMap(stockCheckRepository::create);
        });
    }

    /** 开工：草稿 → 盘点中，记开工时刻。 */
    public Mono<StockCheck> start(Long checkId, String checkNo) {
        return load(checkId, checkNo).flatMap(check -> {
            check.start();
            return stockCheckRepository.save(check);
        });
    }

    /** 填实盘：算差异定级别；正常档留盘点中可直接调账，重大/严重转待审批。 */
    public Mono<StockCheck> count(Long checkId, String checkNo, BigDecimal countedWeight) {
        return load(checkId, checkNo).flatMap(check -> {
            check.count(countedWeight);
            return stockCheckRepository.save(check);
        });
    }

    /** 批准：待审批 → 已批准。 */
    public Mono<StockCheck> approve(Long checkId, String checkNo, String approver) {
        return load(checkId, checkNo).flatMap(check -> {
            check.approve(approver);
            return stockCheckRepository.save(check);
        });
    }

    /** 驳回：待审批 → 已驳回，必须写明理由。 */
    public Mono<StockCheck> reject(Long checkId, String checkNo, String reason) {
        return load(checkId, checkNo).flatMap(check -> {
            check.reject(reason);
            return stockCheckRepository.save(check);
        });
    }

    /**
     * 调账：把账拉平。正常档盘点中直接调；重大/严重需已批准。
     * 一个事务里完成：单子置已调账 + 写一条调账流水 + 写在库对冲批次。
     */
    public Mono<StockCheck> adjust(Long checkId, String checkNo, String reason) {
        return load(checkId, checkNo).flatMap(check -> {
            check.adjust();
            StockAdjust adjust = StockAdjust.of(check, reason);
            return stockAdjustRepository.applyAdjustment(check, adjust).thenReturn(check);
        });
    }

    /** 作废：未终态才能作废；作废是终态，冻结随之解除。 */
    public Mono<StockCheck> cancel(Long checkId, String checkNo) {
        return load(checkId, checkNo).flatMap(check -> {
            check.cancel();
            return stockCheckRepository.save(check);
        });
    }

    public Mono<StockCheck> detail(Long checkId, String checkNo) {
        return load(checkId, checkNo);
    }

    public Mono<PageResult<StockCheck>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                             String checkPeriod, String checkLevel, String status) {
        return stockCheckRepository.page(pageNum, pageSize, sourceId, categoryCode, checkPeriod, checkLevel, status);
    }

    /**
     * 对账：给单位 + 类别 + 月份，回账面 / 实盘 / 差异 / 结存。
     * 账面、实盘、差异取自该期间最新一份未作废盘点单；结存按在库批次实际合计现算。
     */
    public Mono<StockReconciliation> reconcile(Long sourceId, String categoryCode, String checkPeriod) {
        return Mono.defer(() -> {
            if (sourceId == null) {
                return Mono.error(new BizException("产废单位不能为空"));
            }
            if (categoryCode == null || categoryCode.isBlank()) {
                return Mono.error(new BizException("危废类别不能为空"));
            }
            if (checkPeriod == null || checkPeriod.isBlank()) {
                return Mono.error(new BizException("盘点期间不能为空"));
            }
            return stockCheckRepository.findLatestActive(sourceId, categoryCode.trim(), checkPeriod.trim())
                    .switchIfEmpty(Mono.error(new BizException("该单位该类别该期间没有未作废的盘点单")))
                    .flatMap(check -> wasteStockRepository.sumInStock(check.getSourceId(), check.getCategoryCode())
                            .map(balance -> new StockReconciliation(
                                    check.getCheckNo(),
                                    check.getSourceId(),
                                    check.getCategoryCode(),
                                    check.getCheckPeriod(),
                                    check.getStatus(),
                                    check.getBookWeight(),
                                    check.getCountedWeight(),
                                    check.getDiffWeight(),
                                    balance)));
        });
    }

    /** 按 id 或编号加载盘点单；两个都不传或查不到都视为业务失败。 */
    private Mono<StockCheck> load(Long checkId, String checkNo) {
        Mono<StockCheck> found;
        if (checkId != null) {
            found = stockCheckRepository.findById(checkId);
        } else if (checkNo != null && !checkNo.isBlank()) {
            found = stockCheckRepository.findByCheckNo(checkNo.trim());
        } else {
            return Mono.error(new BizException("checkId 或 checkNo 必传其一"));
        }
        return found.switchIfEmpty(Mono.error(new BizException("盘点单不存在")));
    }
}
