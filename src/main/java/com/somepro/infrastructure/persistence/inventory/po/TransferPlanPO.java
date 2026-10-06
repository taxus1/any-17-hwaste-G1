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
 * t_transfer_plan 表的持久化对象（PO，基础设施层）。
 * 联单必须挂靠计划（plan_id 非空）：转出时未指定计划的，按单位+类别+当年查找或补建一张。
 */
@Getter
@Setter
@TableName("t_transfer_plan")
public class TransferPlanPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("plan_no")
    private String planNo;

    @TableField("source_id")
    private Long sourceId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("plan_year")
    private Integer planYear;

    @TableField("planned_weight")
    private BigDecimal plannedWeight;

    @TableField("approved_weight")
    private BigDecimal approvedWeight;

    @TableField("status")
    private String status;
}
