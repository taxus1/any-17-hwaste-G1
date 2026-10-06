package com.somepro.infrastructure.persistence.hwaste;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.hwaste.po.StockAdjustPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * t_stock_adjust 的 MyBatis-Plus Mapper（基础设施层，阻塞 JDBC）。
 */
@Mapper
public interface StockAdjustMapper extends BaseMapper<StockAdjustPO> {

    @Select("SELECT MAX(adjust_no) FROM t_stock_adjust WHERE adjust_no LIKE CONCAT(#{prefix}, '%') FOR UPDATE")
    String maxAdjustNo(@Param("prefix") String prefix);
}
