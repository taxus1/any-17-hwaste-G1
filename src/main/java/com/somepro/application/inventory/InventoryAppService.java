package com.somepro.application.inventory;

import com.somepro.common.exception.BizException;
import com.somepro.domain.inventory.model.TransferManifest;
import com.somepro.domain.inventory.model.WasteStock;
import com.somepro.domain.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 库存应用服务（应用层）：新入库与联单转出。
 *
 * 盘点冻结规则不在此重复判断——仓储实现的 stockIn / transferOut 会在「单位+类别」
 * 组合锁内先查盘点占用态再动账，避免「一边盘一边动账把数搅乱」；这里只做主数据校验与编排。
 */
@Service
public class InventoryAppService {

    private final InventoryRepository inventoryRepository;

    public InventoryAppService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /** 新入库：校验单位与类别存在后入库（盘点占用中的组合会被仓储层挡回）。 */
    public Mono<WasteStock> stockIn(Long sourceId, String categoryCode, String packageType, BigDecimal weightKg) {
        return Mono.zip(
                        inventoryRepository.existsSource(sourceId),
                        inventoryRepository.existsCategory(categoryCode))
                .flatMap(t -> {
                    if (!t.getT1()) {
                        return Mono.error(new BizException("产废单位不存在：" + sourceId));
                    }
                    if (!t.getT2()) {
                        return Mono.error(new BizException("危废类别不存在：" + categoryCode));
                    }
                    return inventoryRepository.stockIn(WasteStock.stockIn(sourceId, categoryCode, packageType, weightKg));
                });
    }

    /** 联单转出：校验单位/类别/处置单位后建单划转（盘点占用中的组合会被仓储层挡回）。 */
    public Mono<TransferManifest> transferOut(Long sourceId, String categoryCode, Long unitId,
                                              BigDecimal transferWeight, String transporter, Long planId) {
        return Mono.zip(
                        inventoryRepository.existsSource(sourceId),
                        inventoryRepository.existsCategory(categoryCode),
                        inventoryRepository.existsUnit(unitId))
                .flatMap(t -> {
                    if (!t.getT1()) {
                        return Mono.error(new BizException("产废单位不存在：" + sourceId));
                    }
                    if (!t.getT2()) {
                        return Mono.error(new BizException("危废类别不存在：" + categoryCode));
                    }
                    if (!t.getT3()) {
                        return Mono.error(new BizException("处置单位不存在：" + unitId));
                    }
                    TransferManifest manifest = TransferManifest.submit(
                            planId, sourceId, unitId, categoryCode, transporter, transferWeight, null);
                    return inventoryRepository.transferOut(manifest);
                });
    }
}
