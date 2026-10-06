package com.somepro.infrastructure.persistence.stockcheck;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.stockcheck.po.StockAdjustPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调账流水的 MyBatis-Plus Mapper（基础设施层）。阻塞 JDBC，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface StockAdjustMapper extends BaseMapper<StockAdjustPO> {
}
