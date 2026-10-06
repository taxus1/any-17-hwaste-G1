package com.somepro.infrastructure.persistence.inventory.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * t_treatment_unit 表的持久化对象（PO，基础设施层）。主数据，本模块只读不写。
 */
@Getter
@Setter
@TableName("t_treatment_unit")
public class TreatmentUnitPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("unit_no")
    private String unitNo;

    @TableField("name")
    private String name;

    @TableField("unit_type")
    private String unitType;

    @TableField("province")
    private String province;

    @TableField("license_no")
    private String licenseNo;

    @TableField("licensed_weight")
    private BigDecimal licensedWeight;

    @TableField("received_weight")
    private BigDecimal receivedWeight;

    @TableField("disposes")
    private String disposes;

    @TableField("status")
    private String status;
}
