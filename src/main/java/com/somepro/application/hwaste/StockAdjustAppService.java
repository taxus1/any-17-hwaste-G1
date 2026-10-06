package com.somepro.application.hwaste;

import com.somepro.domain.hwaste.model.StockAdjust;
import com.somepro.domain.hwaste.repository.StockAdjustRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 调账流水查询用例（应用层）。
 */
@Service
public class StockAdjustAppService {

    private final StockAdjustRepository stockAdjustRepository;

    public StockAdjustAppService(StockAdjustRepository stockAdjustRepository) {
        this.stockAdjustRepository = stockAdjustRepository;
    }

    public Mono<PageResult<StockAdjust>> page(int pageNum, int pageSize, Long checkId, Long sourceId,
                                              String categoryCode) {
        return stockAdjustRepository.page(pageNum, pageSize, checkId, sourceId, categoryCode);
    }
}
