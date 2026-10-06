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
 * t_stock_adjust 表的持久化对象（PO，基础设施层）。
 */
@Getter
@Setter
@TableName("t_stock_adjust")
public class StockAdjustPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("adjust_no")
    private String adjustNo;

    @TableField("check_id")
    private Long checkId;

    @TableField("source_id")
    private Long sourceId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("adjust_weight")
    private BigDecimal adjustWeight;

    @TableField("reason")
    private String reason;

    @TableField("adjusted_at")
    private LocalDateTime adjustedAt;
}
