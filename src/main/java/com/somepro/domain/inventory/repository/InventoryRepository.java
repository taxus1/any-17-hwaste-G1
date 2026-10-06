package com.somepro.domain.inventory.repository;

import com.somepro.domain.inventory.model.TransferManifest;
import com.somepro.domain.inventory.model.WasteStock;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 库存（入库批次 / 联单转出）仓储端口（领域层）。
 *
 * 与盘点的互斥约定（由实现保证）：入库与联单转出全程持有「单位+类别」组合锁，
 * 锁内先查是否存在盘点占用态（COUNTING/PENDING_APPROVAL/APPROVED）的盘点单，
 * 有则拒绝——盘点期间不许一边盘一边动账。
 */
public interface InventoryRepository {

    Mono<Boolean> existsSource(Long sourceId);

    Mono<Boolean> existsCategory(String categoryCode);

    Mono<Boolean> existsUnit(Long unitId);

    /** 该单位+类别当前在库（IN_STOCK）批次重量合计；无批次时为 0。账面取数只认这个口径。 */
    Mono<BigDecimal> sumInStockWeight(Long sourceId, String categoryCode);

    /** 入库：冻结检查 + 取号（WB-2026-0001 式）+ 插入，同一临界区完成。 */
    Mono<WasteStock> stockIn(WasteStock stock);

    /**
     * 联单转出（同一事务）：冻结检查 → 库存合计校验 → 计划挂靠（未指定则按当年查找/补建）
     * → 建联单（EM 号）→ 按入库先后 FIFO 划转在库批次；
     * 末批部分划转时拆分子批（父批保留原重量转为已转出，子批带剩余重量留在库），不改写历史重量。
     */
    Mono<TransferManifest> transferOut(TransferManifest manifest);
}
