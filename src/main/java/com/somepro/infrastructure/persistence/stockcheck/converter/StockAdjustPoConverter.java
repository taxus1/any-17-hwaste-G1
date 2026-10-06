package com.somepro.infrastructure.persistence.stockcheck.converter;

import com.somepro.domain.stockcheck.model.StockAdjust;
import com.somepro.infrastructure.persistence.stockcheck.po.StockAdjustPO;

/**
 * StockAdjustPO（表）↔ StockAdjust（领域）转换器（基础设施层）。
 */
public final class StockAdjustPoConverter {

    private StockAdjustPoConverter() {
    }

    public static StockAdjustPO toPo(StockAdjust domain) {
        StockAdjustPO po = new StockAdjustPO();
        po.setId(domain.getId());
        po.setAdjustNo(domain.getAdjustNo());
        po.setCheckId(domain.getCheckId());
        po.setSourceId(domain.getSourceId());
        po.setCategoryCode(domain.getCategoryCode());
        po.setAdjustWeight(domain.getAdjustWeight());
        po.setReason(domain.getReason());
        po.setAdjustedAt(domain.getAdjustedAt());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static StockAdjust toDomain(StockAdjustPO po) {
        StockAdjust domain = new StockAdjust();
        domain.setId(po.getId());
        domain.setAdjustNo(po.getAdjustNo());
        domain.setCheckId(po.getCheckId());
        domain.setSourceId(po.getSourceId());
        domain.setCategoryCode(po.getCategoryCode());
        domain.setAdjustWeight(po.getAdjustWeight());
        domain.setReason(po.getReason());
        domain.setAdjustedAt(po.getAdjustedAt());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
