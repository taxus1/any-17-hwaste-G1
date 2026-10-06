package com.somepro.infrastructure.persistence.hwaste.converter;

import com.somepro.domain.hwaste.model.CheckLevel;
import com.somepro.domain.hwaste.model.CheckStatus;
import com.somepro.domain.hwaste.model.StockCheck;
import com.somepro.infrastructure.persistence.hwaste.po.StockCheckPO;

/**
 * StockCheckPO（表）↔ StockCheck（领域）转换器（基础设施层）。
 * 状态 / 级别在库里存字符串，在领域里是枚举，互转在这里收口。
 */
public final class StockCheckPoConverter {

    private StockCheckPoConverter() {
    }

    public static StockCheckPO toPo(StockCheck domain) {
        StockCheckPO po = new StockCheckPO();
        po.setId(domain.getId());
        po.setCheckNo(domain.getCheckNo());
        po.setSourceId(domain.getSourceId());
        po.setCategoryCode(domain.getCategoryCode());
        po.setCheckPeriod(domain.getCheckPeriod());
        po.setBookWeight(domain.getBookWeight());
        po.setCountedWeight(domain.getCountedWeight());
        po.setDiffWeight(domain.getDiffWeight());
        po.setDiffRatio(domain.getDiffRatio());
        po.setCheckLevel(domain.getCheckLevel() == null ? null : domain.getCheckLevel().name());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setStartedAt(domain.getStartedAt());
        po.setFinishedAt(domain.getFinishedAt());
        po.setApprover(domain.getApprover());
        po.setApprovedAt(domain.getApprovedAt());
        po.setRejectReason(domain.getRejectReason());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static StockCheck toDomain(StockCheckPO po) {
        StockCheck domain = new StockCheck();
        domain.setId(po.getId());
        domain.setCheckNo(po.getCheckNo());
        domain.setSourceId(po.getSourceId());
        domain.setCategoryCode(po.getCategoryCode());
        domain.setCheckPeriod(po.getCheckPeriod());
        domain.setBookWeight(po.getBookWeight());
        domain.setCountedWeight(po.getCountedWeight());
        domain.setDiffWeight(po.getDiffWeight());
        domain.setDiffRatio(po.getDiffRatio());
        domain.setCheckLevel(po.getCheckLevel() == null ? null : CheckLevel.valueOf(po.getCheckLevel()));
        domain.setStatus(po.getStatus() == null ? null : CheckStatus.valueOf(po.getStatus()));
        domain.setStartedAt(po.getStartedAt());
        domain.setFinishedAt(po.getFinishedAt());
        domain.setApprover(po.getApprover());
        domain.setApprovedAt(po.getApprovedAt());
        domain.setRejectReason(po.getRejectReason());
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
