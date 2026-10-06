package com.somepro.domain.hwaste.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 危废入库批次（纯领域实体）。
 *
 * 账面在库重量 = 该单位该类别下 IN_STOCK 批次重量加总。
 * 盘点调账产生的对冲批次也落在这张表（盘盈正数、盘亏负数），
 * 与历史进出记录分行存放，不改写原有批次。
 */
@Getter
@Setter
public class WasteStock extends BaseEntity {

    private Long id;

    /** 入库批次号，全局唯一，形如 WB-2026-0001（由仓储层分配）。 */
    private String batchNo;

    private Long sourceId;

    private String categoryCode;

    /** 包装：DRUM 桶装 / BAG 袋装 / TANK 罐装 / BULK 散装。 */
    private String packageType;

    private BigDecimal weightKg;

    private LocalDateTime inAt;

    private StockStatus status;

    private Long manifestId;

    /** 拆分出的子批指向被拆的父批。 */
    private Long parentBatchId;

    /** 工厂方法：新入库（默认桶装，落在库状态，记入库时刻）。 */
    public static WasteStock inbound(Long sourceId, String categoryCode, BigDecimal weightKg, String packageType) {
        if (sourceId == null) {
            throw new BizException("产废单位不能为空");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new BizException("危废类别不能为空");
        }
        if (weightKg == null || weightKg.signum() <= 0) {
            throw new BizException("入库重量必须大于 0");
        }
        WasteStock stock = new WasteStock();
        stock.setSourceId(sourceId);
        stock.setCategoryCode(categoryCode.trim());
        stock.setWeightKg(weightKg);
        stock.setPackageType((packageType == null || packageType.isBlank()) ? "DRUM" : packageType.trim());
        stock.setStatus(StockStatus.IN_STOCK);
        stock.setInAt(LocalDateTime.now());
        return stock;
    }
}
