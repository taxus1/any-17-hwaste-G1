package com.somepro.application.hwaste;

import com.somepro.common.exception.BizException;
import com.somepro.domain.hwaste.model.WasteStock;
import com.somepro.domain.hwaste.repository.StockCheckRepository;
import com.somepro.domain.hwaste.repository.WasteStockRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 入库 / 转出用例编排（应用层）。
 *
 * 盘点冻结规则：该单位该类别一旦有单子进到盘点中（COUNTING / PENDING_APPROVAL / APPROVED），
 * 新入库与联单转出都先停下来，等调完账或作废再放行；没在盘点中的组合照常放行。
 */
@Service
public class StockAppService {

    private final WasteStockRepository wasteStockRepository;
    private final StockCheckRepository stockCheckRepository;

    public StockAppService(WasteStockRepository wasteStockRepository, StockCheckRepository stockCheckRepository) {
        this.wasteStockRepository = wasteStockRepository;
        this.stockCheckRepository = stockCheckRepository;
    }

    /** 新入库：盘点冻结中的组合先挡回。 */
    public Mono<WasteStock> inbound(Long sourceId, String categoryCode, BigDecimal weightKg, String packageType) {
        return Mono.defer(() -> {
            WasteStock stock = WasteStock.inbound(sourceId, categoryCode, weightKg, packageType);
            return rejectIfFrozen(stock.getSourceId(), stock.getCategoryCode(), "新入库")
                    .then(wasteStockRepository.inbound(stock));
        });
    }

    /** 联单转出：盘点冻结中的组合先挡回；在库不足也挡回。 */
    public Mono<BigDecimal> transferOut(Long sourceId, String categoryCode, BigDecimal weightKg) {
        return Mono.defer(() -> {
            if (sourceId == null) {
                return Mono.error(new BizException("产废单位不能为空"));
            }
            if (categoryCode == null || categoryCode.isBlank()) {
                return Mono.error(new BizException("危废类别不能为空"));
            }
            if (weightKg == null || weightKg.signum() <= 0) {
                return Mono.error(new BizException("转出重量必须大于 0"));
            }
            return rejectIfFrozen(sourceId, categoryCode.trim(), "联单转出")
                    .then(wasteStockRepository.transferOut(sourceId, categoryCode.trim(), weightKg));
        });
    }

    /** 该单位该类别当前在库重量合计。 */
    public Mono<BigDecimal> sumInStock(Long sourceId, String categoryCode) {
        return Mono.defer(() -> {
            if (sourceId == null) {
                return Mono.error(new BizException("产废单位不能为空"));
            }
            if (categoryCode == null || categoryCode.isBlank()) {
                return Mono.error(new BizException("危废类别不能为空"));
            }
            return wasteStockRepository.sumInStock(sourceId, categoryCode.trim());
        });
    }

    public Mono<PageResult<WasteStock>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                             String status) {
        return wasteStockRepository.page(pageNum, pageSize, sourceId, categoryCode, status);
    }

    /** 盘点冻结校验：该单位该类别有盘点中的单子就挡回。 */
    private Mono<Void> rejectIfFrozen(Long sourceId, String categoryCode, String action) {
        return stockCheckRepository.countFreezing(sourceId, categoryCode)
                .flatMap(frozen -> frozen != null && frozen > 0
                        ? Mono.error(new BizException("该单位该类别正在盘点中，" + action + "暂停，待盘点调账或作废后放行"))
                        : Mono.empty());
    }
}
