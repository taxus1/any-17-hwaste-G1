package com.somepro.infrastructure.persistence.inventory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.inventory.po.TransferPlanPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 年度转移计划的 MyBatis-Plus Mapper（基础设施层）。阻塞 JDBC，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface TransferPlanMapper extends BaseMapper<TransferPlanPO> {
}
