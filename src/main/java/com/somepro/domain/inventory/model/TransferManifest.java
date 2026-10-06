package com.somepro.domain.inventory.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 电子转移联单（领域层）。本模块只覆盖「联单转出」：建单并按 FIFO 把在库批次划转为已转出。
 */
@Getter
@Setter
public class TransferManifest extends BaseEntity {

    private Long id;

    /** 联单编号（EM-2026-0001），落库时由仓储层统一取号。 */
    private String manifestNo;

    /** 年度计划 id；未指定时由仓储层按单位+类别+当年查找或补建。 */
    private Long planId;

    private Long sourceId;

    private Long unitId;

    private String categoryCode;

    private String transporter;

    /** 申报转移重量（千克）。 */
    private BigDecimal transferWeight;

    /** 是否跨省：建单时按类别名录快照（1 是 / 0 否）。 */
    private Integer crossProvince;

    private ManifestStatus status;

    /** 工厂方法：联单转出申请，落 SUBMITTED。 */
    public static TransferManifest submit(Long planId, Long sourceId, Long unitId, String categoryCode,
                                          String transporter, BigDecimal transferWeight, Integer crossProvince) {
        if (sourceId == null) {
            throw new BizException("产废单位不能为空");
        }
        if (unitId == null) {
            throw new BizException("处置单位不能为空");
        }
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new BizException("危废类别不能为空");
        }
        if (transferWeight == null || transferWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("转移重量必须大于 0");
        }
        TransferManifest manifest = new TransferManifest();
        manifest.setPlanId(planId);
        manifest.setSourceId(sourceId);
        manifest.setUnitId(unitId);
        manifest.setCategoryCode(categoryCode.trim());
        manifest.setTransporter(transporter);
        manifest.setTransferWeight(transferWeight);
        manifest.setCrossProvince(crossProvince == null ? 0 : crossProvince);
        manifest.setStatus(ManifestStatus.SUBMITTED);
        return manifest;
    }
}
