package com.somepro.domain.stockcheck.repository;

import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.stockcheck.model.CheckLevel;
import com.somepro.domain.stockcheck.model.CheckStatus;
import com.somepro.domain.stockcheck.model.StockAdjust;
import com.somepro.domain.stockcheck.model.StockCheck;
import com.somepro.domain.inventory.model.WasteStock;
import reactor.core.publisher.Mono;

/**
 * 盘点单仓储端口（领域层）。实现见基础设施层 StockCheckRepositoryImpl。
 *
 * 与并发相关的两条硬约定（由实现保证）：
 * - createIfAbsent：同一单位+类别+月份只准落一张未作废的单，查重与插入在同一临界区；
 * - updateWithExpectedStatus / adjustInTransaction：以前置状态为条件的更新，
 *   并发下只有一个请求能推进状态，其余失败。
 */
public interface StockCheckRepository {

    /**
     * 立单：同一单位+类别+月份若已存在未作废的盘点单则抛业务异常；
     * 否则取号（CK-2026-0001 式）并插入，返回带编号与 id 的完整对象。
     */
    Mono<StockCheck> createIfAbsent(StockCheck check);

    Mono<StockCheck> findById(Long id);

    /** 查该组合下未作废的盘点单（立单规则保证最多一张）。 */
    Mono<StockCheck> findActiveByCombo(Long sourceId, String categoryCode, String checkPeriod);

    /** 该单位+类别当前处于「盘点占用」态（COUNTING/PENDING_APPROVAL/APPROVED）的单数，>0 即应冻结进出。 */
    Mono<Long> countBlocking(Long sourceId, String categoryCode);

    /** 分页查询：过滤条件全部可空，啥都不挑就分页列全。 */
    Mono<PageResult<StockCheck>> page(int pageNum, int pageSize,
                                      Long sourceId, String categoryCode, String checkPeriod,
                                      CheckLevel checkLevel, CheckStatus status);

    /**
     * 以前置状态为条件回写领域对象（状态机流转用）：
     * 仅当库中当前状态等于 expected 才更新；expected 传 null 表示「任意非终态」均可（作废用）。
     * 返回是否命中（false 说明状态已被别人改动）。
     */
    Mono<Boolean> updateWithExpectedStatus(StockCheck check, CheckStatus expected);

    /**
     * 调账事务：同一事务内完成——
     * 1) 插调账流水（uk_adjust_check 兜底，重复调账整体回滚）；
     * 2) 插盘点调整批次（adjustBatch 为 null 表示差异为 0、无需动库存）；
     * 3) 盘点单状态 APPROVED → ADJUSTED（前置状态条件更新）。
     * 历史批次一律不改写，调整只新增痕迹。
     */
    Mono<StockAdjust> adjustInTransaction(StockCheck check, StockAdjust adjust, WasteStock adjustBatch);
}
