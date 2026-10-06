package com.somepro.infrastructure.persistence.inventory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.inventory.po.WasteCategoryPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 危废类别名录的 MyBatis-Plus Mapper（基础设施层）。阻塞 JDBC，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface WasteCategoryMapper extends BaseMapper<WasteCategoryPO> {
}
