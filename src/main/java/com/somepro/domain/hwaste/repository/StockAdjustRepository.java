package com.somepro.domain.hwaste.repository;

import com.somepro.domain.hwaste.model.StockAdjust;
import com.somepro.domain.hwaste.model.StockCheck;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 调账流水仓储端口：领域层定义，基础设施层实现。
 */
public interface StockAdjustRepository {

    /**
     * 调账落库（一个事务里把账拉平）：
     * 1. 盘点单状态条件更新为已调账（防并发重复调账）；
     * 2. 写一条调账流水（盘盈正、盘亏负，uk_adjust_check 保证一单一流水）；
     * 3. 写在库对冲批次（盘盈补正数、盘亏补负数），历史批次不动。
     */
    Mono<StockAdjust> applyAdjustment(StockCheck check, StockAdjust adjust);

    /** 多条件分页查调账流水：盘点单 / 单位 / 类别均可选。 */
    Mono<PageResult<StockAdjust>> page(int pageNum, int pageSize, Long checkId, Long sourceId, String categoryCode);
}
