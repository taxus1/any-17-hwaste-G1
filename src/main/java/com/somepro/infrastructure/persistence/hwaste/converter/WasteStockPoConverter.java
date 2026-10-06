package com.somepro.infrastructure.persistence.hwaste.converter;

import com.somepro.domain.hwaste.model.StockStatus;
import com.somepro.domain.hwaste.model.WasteStock;
import com.somepro.infrastructure.persistence.hwaste.po.WasteStockPO;

/**
 * WasteStockPO（表）↔ WasteStock（领域）转换器（基础设施层）。
 */
public final class WasteStockPoConverter {

    private WasteStockPoConverter() {
    }

    public static WasteStockPO toPo(WasteStock domain) {
        WasteStockPO po = new WasteStockPO();
        po.setId(domain.getId());
        po.setBatchNo(domain.getBatchNo());
        po.setSourceId(domain.getSourceId());
        po.setCategoryCode(domain.getCategoryCode());
        po.setPackageType(domain.getPackageType());
        po.setWeightKg(domain.getWeightKg());
        po.setInAt(domain.getInAt());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setManifestId(domain.getManifestId());
        po.setParentBatchId(domain.getParentBatchId());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static WasteStock toDomain(WasteStockPO po) {
        WasteStock domain = new WasteStock();
        domain.setId(po.getId());
        domain.setBatchNo(po.getBatchNo());
        domain.setSourceId(po.getSourceId());
        domain.setCategoryCode(po.getCategoryCode());
        domain.setPackageType(po.getPackageType());
        domain.setWeightKg(po.getWeightKg());
        domain.setInAt(po.getInAt());
        domain.setStatus(po.getStatus() == null ? null : StockStatus.valueOf(po.getStatus()));
        domain.setManifestId(po.getManifestId());
        domain.setParentBatchId(po.getParentBatchId());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
