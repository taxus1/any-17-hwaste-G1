package com.somepro.infrastructure.persistence.inventory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.inventory.po.TreatmentUnitPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 处置利用单位的 MyBatis-Plus Mapper（基础设施层）。阻塞 JDBC，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface TreatmentUnitMapper extends BaseMapper<TreatmentUnitPO> {
}
