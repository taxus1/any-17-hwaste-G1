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
 * t_stock_check 表的持久化对象（PO，基础设施层）。只描述表形状，不放业务规则。
 */
@Getter
@Setter
@TableName("t_stock_check")
public class StockCheckPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("check_no")
    private String checkNo;

    @TableField("source_id")
    private Long sourceId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("check_period")
    private String checkPeriod;

    @TableField("book_weight")
    private BigDecimal bookWeight;

    @TableField("counted_weight")
    private BigDecimal countedWeight;

    @TableField("diff_weight")
    private BigDecimal diffWeight;

    @TableField("diff_ratio")
    private BigDecimal diffRatio;

    @TableField("check_level")
    private String checkLevel;

    @TableField("status")
    private String status;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("approver")
    private String approver;

    @TableField("approved_at")
    private LocalDateTime approvedAt;

    @TableField("reject_reason")
    private String rejectReason;
}
