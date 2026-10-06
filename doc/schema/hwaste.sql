-- any-17-hwaste · 危险废物电子转移联单与库存核销单业务线 · 建表 SQL
-- 字符集 utf8mb4，时区 Asia/Shanghai。create 阶段建好，模型只写业务代码，不碰建表。
-- 列名即契约：del_flag 由 @TableLogic 自动拼接（查询带 del_flag=0，删除置 1），
-- create_by/update_by/create_time/update_time 由 AutoFillMetaObjectHandler 自动填充，业务代码不要手写。
-- 主键 id 由应用侧雪花分配（IdType.INPUT），不依赖自增。重量单位一律千克。

-- 1) 产废单位
CREATE TABLE IF NOT EXISTS t_waste_source (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    source_no   VARCHAR(32)   NOT NULL COMMENT '产废单位编号，全局唯一（如 WS-2026-0001）',
    name        VARCHAR(128)  NOT NULL COMMENT '单位名称',
    credit_code VARCHAR(64)   NOT NULL COMMENT '统一社会信用代码',
    province    VARCHAR(32)   NOT NULL COMMENT '所在省份',
    city        VARCHAR(64)   DEFAULT NULL COMMENT '所在城市',
    address     VARCHAR(255)  DEFAULT NULL COMMENT '详细地址',
    contact     VARCHAR(64)   DEFAULT NULL COMMENT '联系人',
    phone       VARCHAR(20)   DEFAULT NULL COMMENT '联系电话',
    status      VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 正常 / SUSPENDED 停用 / CLOSED 关闭',
    del_flag    TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by   VARCHAR(64)   DEFAULT NULL,
    create_time DATETIME      DEFAULT NULL,
    update_by   VARCHAR(64)   DEFAULT NULL,
    update_time DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_source_no (source_no),
    KEY idx_status (status),
    KEY idx_province (province)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产废单位';

-- 2) 危险废物类别名录
CREATE TABLE IF NOT EXISTS t_waste_category (
    id             BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    category_code  VARCHAR(16)  NOT NULL COMMENT '危废类别代码，全局唯一（如 HW08）',
    name           VARCHAR(128) NOT NULL COMMENT '类别名称',
    hazard_type    VARCHAR(16)  NOT NULL DEFAULT 'TOXIC' COMMENT '危险特性 TOXIC 毒性 / CORROSIVE 腐蚀性 / FLAMMABLE 易燃 / REACTIVE 反应性 / INFECTIOUS 感染性',
    cross_province TINYINT      NOT NULL DEFAULT 0 COMMENT '是否限制跨省转移：1 限制（不得跨省）/ 0 允许',
    status         VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED 启用 / DISABLED 停用',
    del_flag       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by      VARCHAR(64)  DEFAULT NULL,
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT NULL,
    update_time    DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_category_code (category_code),
    KEY idx_status (status),
    KEY idx_hazard (hazard_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危险废物类别名录';

-- 3) 处置利用单位
CREATE TABLE IF NOT EXISTS t_treatment_unit (
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    unit_no         VARCHAR(32)   NOT NULL COMMENT '处置单位编号，全局唯一（如 TU-2026-0001）',
    name            VARCHAR(128)  NOT NULL COMMENT '单位名称',
    unit_type       VARCHAR(16)   NOT NULL DEFAULT 'DISPOSAL' COMMENT '类型 DISPOSAL 处置 / UTILIZATION 利用',
    province        VARCHAR(32)   NOT NULL COMMENT '所在省份',
    license_no      VARCHAR(64)   DEFAULT NULL COMMENT '经营许可证号',
    licensed_weight DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '许可经营重量上限（千克）',
    received_weight DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '累计已接收重量（千克，签收时累加）',
    disposes            VARCHAR(255)  DEFAULT NULL COMMENT '可处置类别代码，逗号分隔（如 HW08,HW09）',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 正常 / SUSPENDED 停用 / REVOKED 吊销',
    del_flag        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by       VARCHAR(64)   DEFAULT NULL,
    create_time     DATETIME      DEFAULT NULL,
    update_by       VARCHAR(64)   DEFAULT NULL,
    update_time     DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_unit_no (unit_no),
    KEY idx_status (status),
    KEY idx_province (province)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处置利用单位';

-- 4) 危废入库批次
CREATE TABLE IF NOT EXISTS t_waste_stock (
    id           BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    batch_no     VARCHAR(32)   NOT NULL COMMENT '入库批次号，全局唯一（如 WB-2026-0001）',
    source_id    BIGINT        NOT NULL COMMENT '产废单位 id（t_waste_source.id）',
    category_code VARCHAR(16)  NOT NULL COMMENT '危废类别代码（t_waste_category.category_code）',
    package_type VARCHAR(16)   NOT NULL DEFAULT 'DRUM' COMMENT '包装 DRUM 桶装 / BAG 袋装 / TANK 罐装 / BULK 散装',
    weight_kg    DECIMAL(14,2) NOT NULL COMMENT '入库重量（千克）',
    in_at        DATETIME      DEFAULT NULL COMMENT '入库时刻',
    status       VARCHAR(16)   NOT NULL DEFAULT 'IN_STOCK' COMMENT 'IN_STOCK 在库 / TRANSFERRED 已转出 / DISPOSED 已处置 / VOID 已作废',
    manifest_id  BIGINT        DEFAULT NULL COMMENT '转出时关联的联单 id（t_transfer_manifest.id）',
    parent_batch_id BIGINT     DEFAULT NULL COMMENT '来源批次 id（拆分出的子批指向被拆的父批，t_waste_stock.id）',
    del_flag     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by    VARCHAR(64)   DEFAULT NULL,
    create_time  DATETIME      DEFAULT NULL,
    update_by    VARCHAR(64)   DEFAULT NULL,
    update_time  DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_source (source_id),
    KEY idx_category (category_code),
    KEY idx_status (status),
    KEY idx_manifest (manifest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危废入库批次';

-- 5) 年度转移计划
CREATE TABLE IF NOT EXISTS t_transfer_plan (
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    plan_no         VARCHAR(32)   NOT NULL COMMENT '计划编号，全局唯一（如 TP-2026-0001）',
    source_id       BIGINT        NOT NULL COMMENT '产废单位 id（t_waste_source.id）',
    category_code   VARCHAR(16)   NOT NULL COMMENT '危废类别代码（t_waste_category.category_code）',
    plan_year       INT           NOT NULL COMMENT '计划年度',
    planned_weight  DECIMAL(14,2) NOT NULL COMMENT '申报转移重量（千克）',
    approved_weight DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '批复转移重量（千克）',
    status          VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT 草稿 / SUBMITTED 已申报 / APPROVED 已批复 / REJECTED 已驳回',
    del_flag        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by       VARCHAR(64)   DEFAULT NULL,
    create_time     DATETIME      DEFAULT NULL,
    update_by       VARCHAR(64)   DEFAULT NULL,
    update_time     DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_plan_no (plan_no),
    KEY idx_source (source_id),
    KEY idx_category (category_code),
    KEY idx_year (plan_year),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度转移计划';

-- 6) 电子转移联单
CREATE TABLE IF NOT EXISTS t_transfer_manifest (
    id               BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    manifest_no      VARCHAR(32)   NOT NULL COMMENT '联单编号，全局唯一（如 EM-2026-0001）',
    plan_id          BIGINT        NOT NULL COMMENT '年度计划 id（t_transfer_plan.id）',
    source_id        BIGINT        NOT NULL COMMENT '产废单位 id（t_waste_source.id）',
    unit_id          BIGINT        NOT NULL COMMENT '处置单位 id（t_treatment_unit.id）',
    category_code    VARCHAR(16)   NOT NULL COMMENT '危废类别代码（t_waste_category.category_code）',
    transporter      VARCHAR(128)  DEFAULT NULL COMMENT '运输单位名称',
    transfer_weight  DECIMAL(14,2) NOT NULL COMMENT '申报转移重量（千克）',
    cross_province   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否跨省：提交时按类别名录快照，1 是 / 0 否',
    transport_begin  DATETIME      DEFAULT NULL COMMENT '启运时刻',
    transport_end    DATETIME      DEFAULT NULL COMMENT '运抵时刻',
    receive_at       DATETIME      DEFAULT NULL COMMENT '签收时刻',
    status           VARCHAR(16)   NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED 已提交 / APPROVED 已审批 / REJECTED 已退回 / IN_TRANSIT 运输中 / RECEIVED 已签收 / DISPOSED 已处置 / VOID 已作废',
    del_flag         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by        VARCHAR(64)   DEFAULT NULL,
    create_time      DATETIME      DEFAULT NULL,
    update_by        VARCHAR(64)   DEFAULT NULL,
    update_time      DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_manifest_no (manifest_no),
    KEY idx_plan (plan_id),
    KEY idx_source (source_id),
    KEY idx_unit (unit_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子转移联单';

-- 7) 签收与处置确认
CREATE TABLE IF NOT EXISTS t_manifest_signoff (
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    signoff_no      VARCHAR(32)   NOT NULL COMMENT '签收单编号，全局唯一（如 SO-2026-0001）',
    manifest_id     BIGINT        NOT NULL COMMENT '联单 id（t_transfer_manifest.id）',
    received_weight DECIMAL(14,2) NOT NULL COMMENT '实际签收重量（千克）',
    disposed_weight DECIMAL(14,2) DEFAULT NULL COMMENT '实际处置重量（千克）',
    disposal_method VARCHAR(16)   DEFAULT NULL COMMENT '处置方式 INCINERATE 焚烧 / LANDFILL 填埋 / UTILIZE 利用 / CEMENT 水泥窑协同 / OTHER 其他',
    sign_at         DATETIME      DEFAULT NULL COMMENT '签收时刻',
    confirm_at      DATETIME      DEFAULT NULL COMMENT '处置确认时刻',
    del_flag        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by       VARCHAR(64)   DEFAULT NULL,
    create_time     DATETIME      DEFAULT NULL,
    update_by       VARCHAR(64)   DEFAULT NULL,
    update_time     DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_signoff_no (signoff_no),
    KEY idx_manifest (manifest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签收与处置确认';

-- 8) 危废异常预警
CREATE TABLE IF NOT EXISTS t_waste_alert (
    id          BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    alert_no    VARCHAR(32)  NOT NULL COMMENT '预警编号，全局唯一（如 WA-2026-0001）',
    manifest_id BIGINT       NOT NULL COMMENT '联单 id（t_transfer_manifest.id）',
    alert_type  VARCHAR(16)  NOT NULL COMMENT '类型 OVERDUE 在途超期 / WEIGHT_DIFF 重量差异 / QUOTA 许可超限',
    alert_level VARCHAR(16)  NOT NULL DEFAULT 'LOW' COMMENT '级别 LOW 低 / MEDIUM 中 / HIGH 高',
    status      VARCHAR(16)  NOT NULL DEFAULT 'RAISED' COMMENT 'RAISED 已发布 / HANDLING 处置中 / CLOSED 已关闭',
    detail      VARCHAR(255) DEFAULT NULL COMMENT '预警说明',
    raised_at   DATETIME     DEFAULT NULL COMMENT '预警时刻',
    closed_at   DATETIME     DEFAULT NULL COMMENT '关闭时刻',
    next_due_at DATETIME     DEFAULT NULL COMMENT '下次处置到点时刻（按级别时限推算）',
    escalate_count INT       NOT NULL DEFAULT 0 COMMENT '已升级次数',
    del_flag    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by   VARCHAR(64)  DEFAULT NULL,
    create_time DATETIME     DEFAULT NULL,
    update_by   VARCHAR(64)  DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_alert_no (alert_no),
    KEY idx_manifest (manifest_id),
    KEY idx_type (alert_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危废异常预警';

-- 9) 危废库存盘点单
CREATE TABLE IF NOT EXISTS t_stock_check (
    id             BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    check_no       VARCHAR(32)   NOT NULL COMMENT '盘点单编号，全局唯一（如 CK-2026-0001）',
    source_id      BIGINT        NOT NULL COMMENT '产废单位 id（t_waste_source.id）',
    category_code  VARCHAR(16)   NOT NULL COMMENT '危废类别代码（t_waste_category.category_code）',
    check_period   VARCHAR(7)    NOT NULL COMMENT '盘点期间，形如 2026-08',
    book_weight    DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '账面在库重量（立单时按在库批次合计取数）',
    counted_weight DECIMAL(14,2) DEFAULT NULL COMMENT '实盘重量',
    diff_weight    DECIMAL(14,2) DEFAULT NULL COMMENT '差异重量（实盘 - 账面）',
    diff_ratio     DECIMAL(8,4)  DEFAULT NULL COMMENT '差异比例（|差异| / 账面，账面为 0 时记 0）',
    check_level    VARCHAR(16)   DEFAULT NULL COMMENT '差异级别 NORMAL 正常 / MAJOR 重大 / CRITICAL 严重',
    status         VARCHAR(24)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT 草稿 / COUNTING 盘点中 / PENDING_APPROVAL 待审批 / APPROVED 已批准 / REJECTED 已驳回 / ADJUSTED 已调账 / CANCELLED 已作废',
    started_at     DATETIME      DEFAULT NULL COMMENT '开始盘点时刻',
    finished_at    DATETIME      DEFAULT NULL COMMENT '盘点完成时刻',
    approver       VARCHAR(64)   DEFAULT NULL COMMENT '审批人',
    approved_at    DATETIME      DEFAULT NULL COMMENT '审批时刻',
    reject_reason  VARCHAR(255)  DEFAULT NULL COMMENT '驳回理由',
    del_flag       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by      VARCHAR(64)   DEFAULT NULL,
    create_time    DATETIME      DEFAULT NULL,
    update_by      VARCHAR(64)   DEFAULT NULL,
    update_time    DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_check_no (check_no),
    KEY idx_source (source_id),
    KEY idx_category (category_code),
    KEY idx_period (check_period),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危废库存盘点单';

-- 10) 库存调账流水
CREATE TABLE IF NOT EXISTS t_stock_adjust (
    id            BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    adjust_no     VARCHAR(32)   NOT NULL COMMENT '调账流水号，全局唯一（如 AJ-2026-0001）',
    check_id      BIGINT        NOT NULL COMMENT '盘点单 id（t_stock_check.id）',
    source_id     BIGINT        NOT NULL COMMENT '产废单位 id（t_waste_source.id）',
    category_code VARCHAR(16)   NOT NULL COMMENT '危废类别代码（t_waste_category.category_code）',
    adjust_weight DECIMAL(14,2) NOT NULL COMMENT '调账重量，正为盘盈、负为盘亏',
    reason        VARCHAR(255)  DEFAULT NULL COMMENT '调账缘由',
    adjusted_at   DATETIME      DEFAULT NULL COMMENT '调账时刻',
    del_flag      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by     VARCHAR(64)   DEFAULT NULL,
    create_time   DATETIME      DEFAULT NULL,
    update_by     VARCHAR(64)   DEFAULT NULL,
    update_time   DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_adjust_no (adjust_no),
    UNIQUE KEY uk_adjust_check (check_id),
    KEY idx_source (source_id),
    KEY idx_category (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调账流水';
