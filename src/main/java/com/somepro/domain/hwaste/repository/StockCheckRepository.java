package com.somepro.domain.hwaste.repository;

import com.somepro.domain.hwaste.model.StockCheck;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 盘点单仓储端口：领域层定义，基础设施层实现。
 *
 * 立单的「同组合唯一 + 编号分配 + 账面快照落库」要保证并发安全，
 * 由实现侧用锁与唯一约束兜底（两个人同时立单只该成一份）。
 */
public interface StockCheckRepository {

    /**
     * 立单：同一单位 + 类别 + 月份只准挂一份未作废的盘点单，重复立单抛业务异常。
     * 编号（CK-年份-序号）由实现侧分配。
     */
    Mono<StockCheck> create(StockCheck check);

    Mono<StockCheck> findById(Long id);

    Mono<StockCheck> findByCheckNo(String checkNo);

    /** 状态流转落库（按 id 更新）。 */
    Mono<StockCheck> save(StockCheck check);

    /** 多条件分页：单位 / 类别 / 月份 / 级别 / 状态均可选，都不传则分页列全。 */
    Mono<PageResult<StockCheck>> page(int pageNum, int pageSize, Long sourceId, String categoryCode,
                                      String checkPeriod, String checkLevel, String status);

    /** 该单位该类别该期间最新一份未作废的盘点单（对账用）；没有则空。 */
    Mono<StockCheck> findLatestActive(Long sourceId, String categoryCode, String checkPeriod);

    /**
     * 该单位该类别当前处于「盘点冻结中」的单数（COUNTING / PENDING_APPROVAL / APPROVED）。
     * 大于 0 时新入库与联单转出都得先停下来。
     */
    Mono<Long> countFreezing(Long sourceId, String categoryCode);
}
