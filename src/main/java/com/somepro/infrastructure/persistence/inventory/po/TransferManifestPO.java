package com.somepro.infrastructure.persistence.inventory.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * t_transfer_manifest 表的持久化对象（PO，基础设施层）。
 */
@Getter
@Setter
@TableName("t_transfer_manifest")
public class TransferManifestPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("manifest_no")
    private String manifestNo;

    @TableField("plan_id")
    private Long planId;

    @TableField("source_id")
    private Long sourceId;

    @TableField("unit_id")
    private Long unitId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("transporter")
    private String transporter;

    @TableField("transfer_weight")
    private BigDecimal transferWeight;

    @TableField("cross_province")
    private Integer crossProvince;

    @TableField("transport_begin")
    private LocalDateTime transportBegin;

    @TableField("transport_end")
    private LocalDateTime transportEnd;

    @TableField("receive_at")
    private LocalDateTime receiveAt;

    @TableField("status")
    private String status;
}
