package com.somepro.infrastructure.persistence.inventory.converter;

import com.somepro.domain.inventory.model.ManifestStatus;
import com.somepro.domain.inventory.model.TransferManifest;
import com.somepro.infrastructure.persistence.inventory.po.TransferManifestPO;

/**
 * TransferManifestPO（表）↔ TransferManifest（领域）转换器（基础设施层）。
 */
public final class TransferManifestPoConverter {

    private TransferManifestPoConverter() {
    }

    public static TransferManifestPO toPo(TransferManifest domain) {
        TransferManifestPO po = new TransferManifestPO();
        po.setId(domain.getId());
        po.setManifestNo(domain.getManifestNo());
        po.setPlanId(domain.getPlanId());
        po.setSourceId(domain.getSourceId());
        po.setUnitId(domain.getUnitId());
        po.setCategoryCode(domain.getCategoryCode());
        po.setTransporter(domain.getTransporter());
        po.setTransferWeight(domain.getTransferWeight());
        po.setCrossProvince(domain.getCrossProvince());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static TransferManifest toDomain(TransferManifestPO po) {
        TransferManifest domain = new TransferManifest();
        domain.setId(po.getId());
        domain.setManifestNo(po.getManifestNo());
        domain.setPlanId(po.getPlanId());
        domain.setSourceId(po.getSourceId());
        domain.setUnitId(po.getUnitId());
        domain.setCategoryCode(po.getCategoryCode());
        domain.setTransporter(po.getTransporter());
        domain.setTransferWeight(po.getTransferWeight());
        domain.setCrossProvince(po.getCrossProvince());
        domain.setStatus(po.getStatus() == null ? null : ManifestStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
