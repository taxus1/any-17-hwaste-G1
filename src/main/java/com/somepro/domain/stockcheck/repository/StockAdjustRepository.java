package com.somepro.domain.stockcheck.repository;

import com.somepro.domain.stockcheck.model.StockAdjust;
import reactor.core.publisher.Mono;

/**
 * 调账流水仓储端口（领域层）。流水的写入走 StockCheckRepository.adjustInTransaction（同事务），
 * 这里只提供查询。
 */
public interface StockAdjustRepository {

    /** 按盘点单查调账流水（同一张单最多一条，查不到返回空信号）。 */
    Mono<StockAdjust> findByCheckId(Long checkId);
}
