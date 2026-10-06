package com.somepro.interfaces.rest.inventory;

import com.somepro.application.inventory.InventoryAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.inventory.converter.InventoryVoConverter;
import com.somepro.interfaces.rest.inventory.vo.ManifestVO;
import com.somepro.interfaces.rest.inventory.vo.WasteStockVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * 库存进出接口（用户接口层）：新入库与联单转出。
 * 该单位+类别一旦进入盘点（COUNTING 起），这两个动作会被冻结，直到盘点调账或作废。
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryAppService inventoryAppService;

    public InventoryController(InventoryAppService inventoryAppService) {
        this.inventoryAppService = inventoryAppService;
    }

    /** 新入库：生成在库批次（WB 号）；盘点占用中的组合会被挡回。 */
    @PostMapping("/stock-in")
    public Mono<Result<WasteStockVO>> stockIn(@RequestParam Long sourceId,
                                              @RequestParam String categoryCode,
                                              @RequestParam BigDecimal weightKg,
                                              @RequestParam(required = false) String packageType) {
        return inventoryAppService.stockIn(sourceId, categoryCode, packageType, weightKg)
                .map(InventoryVoConverter::toVo)
                .map(Result::ok);
    }

    /** 联单转出：建联单（EM 号）并按 FIFO 划转在库批次；盘点占用中的组合会被挡回。 */
    @PostMapping("/transfer-out")
    public Mono<Result<ManifestVO>> transferOut(@RequestParam Long sourceId,
                                                @RequestParam String categoryCode,
                                                @RequestParam Long unitId,
                                                @RequestParam BigDecimal transferWeight,
                                                @RequestParam(required = false) String transporter,
                                                @RequestParam(required = false) Long planId) {
        return inventoryAppService.transferOut(sourceId, categoryCode, unitId, transferWeight, transporter, planId)
                .map(InventoryVoConverter::toVo)
                .map(Result::ok);
    }
}
