package com.somepro.infrastructure.persistence.hwaste.po;

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
 * t_waste_stock 表的持久化对象（PO，基础设施层）。
 */
@Getter
@Setter
@TableName("t_waste_stock")
public class WasteStockPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("batch_no")
    private String batchNo;

    @TableField("source_id")
    private Long sourceId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("package_type")
    private String packageType;

    @TableField("weight_kg")
    private BigDecimal weightKg;

    @TableField("in_at")
    private LocalDateTime inAt;

    @TableField("status")
    private String status;

    @TableField("manifest_id")
    private Long manifestId;

    @TableField("parent_batch_id")
    private Long parentBatchId;
}
