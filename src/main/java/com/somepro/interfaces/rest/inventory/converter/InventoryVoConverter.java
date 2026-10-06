package com.somepro.interfaces.rest.inventory.converter;

import com.somepro.domain.inventory.model.TransferManifest;
import com.somepro.domain.inventory.model.WasteStock;
import com.somepro.interfaces.rest.inventory.vo.ManifestVO;
import com.somepro.interfaces.rest.inventory.vo.WasteStockVO;

/**
 * 库存领域对象 → 对外 VO 转换器（用户接口层）。
 */
public final class InventoryVoConverter {

    private InventoryVoConverter() {
    }

    public static WasteStockVO toVo(WasteStock domain) {
        return new WasteStockVO(
                domain.getId(),
                domain.getBatchNo(),
                domain.getSourceId(),
                domain.getCategoryCode(),
                domain.getPackageType(),
                domain.getWeightKg(),
                domain.getInAt(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getManifestId(),
                domain.getParentBatchId(),
                domain.getCreateTime());
    }

    public static ManifestVO toVo(TransferManifest domain) {
        return new ManifestVO(
                domain.getId(),
                domain.getManifestNo(),
                domain.getPlanId(),
                domain.getSourceId(),
                domain.getUnitId(),
                domain.getCategoryCode(),
                domain.getTransporter(),
                domain.getTransferWeight(),
                domain.getCrossProvince(),
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }
}
