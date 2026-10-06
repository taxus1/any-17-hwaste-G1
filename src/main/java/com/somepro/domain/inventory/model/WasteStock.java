package com.somepro.domain.inventory.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 危废入库批次（领域层）。
 *
 * 本模块用它承载两类批次：
 * - 正常入库批（stockIn）：正重量，IN_STOCK；
 * - 盘点调整批（adjustment）：盘盈为正、盘亏为负，IN_STOCK，作为盘点调整的独立痕迹
 *   插入，历史批次的原始进出记录一律不改写。
 */
@Getter
@Setter
public class WasteStock extends BaseEntity {

    private Long id;

    /** 入库批次号（WB-2026-0001），落库时由仓储层统一取号。 */
    private String batchNo;

    private Long sourceId;

    private String categoryCode;

    /** 包装：DRUM 桶装 / BAG 袋装 / TANK 罐装 / BULK 散装。 */
    private String packageType;

    /** 重量（千克）；盘点调整批盘亏时为负。 */
    private BigDecimal weightKg;

    private LocalDateTime inAt;

    private BatchStatus status;

    /** 转出时关联的联单 id。 */
    private Long manifestId;

    /** 拆分时子批指向的父批 id。 */
    private Long parentBatchId;

    /** 工厂方法：正常入库批。 */
    public static WasteStock stockIn(Long sourceId, String categoryCode, String packageType, BigDecimal weightKg) {
        if (sourceId == null) {
            throw new BizException("产废单位不能为空");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new BizException("危废类别不能为空");
        }
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("入库重量必须大于 0");
        }
        WasteStock stock = new WasteStock();
        stock.setSourceId(sourceId);
        stock.setCategoryCode(categoryCode.trim());
        stock.setPackageType((packageType == null || packageType.isBlank()) ? "DRUM" : packageType.trim());
        stock.setWeightKg(weightKg);
        stock.setInAt(LocalDateTime.now());
        stock.setStatus(BatchStatus.IN_STOCK);
        return stock;
    }

    /** 工厂方法：盘点调整批（盘盈正、盘亏负），只新增痕迹、不动历史批次。 */
    public static WasteStock adjustment(Long sourceId, String categoryCode, BigDecimal adjustWeight) {
        WasteStock stock = new WasteStock();
        stock.setSourceId(sourceId);
        stock.setCategoryCode(categoryCode);
        stock.setPackageType("DRUM");
        stock.setWeightKg(adjustWeight);
        stock.setInAt(LocalDateTime.now());
        stock.setStatus(BatchStatus.IN_STOCK);
        return stock;
    }
}
