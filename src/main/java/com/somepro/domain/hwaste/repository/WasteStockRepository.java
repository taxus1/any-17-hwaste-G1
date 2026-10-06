package com.somepro.domain.hwaste.repository;

import com.somepro.domain.hwaste.model.WasteStock;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 入库批次仓储端口：领域层定义，基础设施层实现。
 */
public interface WasteStockRepository {

    /** 新入库：分配批次号（WB-年份-序号）并落库。 */
    Mono<WasteStock> inbound(WasteStock stock);

    /** 该单位该类别当前在库（IN_STOCK）批次重量合计；没有则为 0。 */
    Mono<BigDecimal> sumInStock(Long sourceId, String categoryCode);

    /**
     * 联单转出：按入库先后 FIFO 消化在库批次，不足整批的拆分子批。
     * 在库合计不足时抛业务异常；返回实际转出重量。
     */
    Mono<BigDecimal> transferOut(Long sourceId, String categoryCode, BigDecimal weightKg);

    /** 多条件分页查批次：单位 / 类别 / 状态均可选。 */
    Mono<PageResult<WasteStock>> page(int pageNum, int pageSize, Long sourceId, String categoryCode, String status);
}
