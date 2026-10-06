package com.somepro.infrastructure.persistence.hwaste;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.hwaste.po.StockCheckPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * t_stock_check 的 MyBatis-Plus Mapper（基础设施层，阻塞 JDBC，只能在 boundedElastic 线程上调用）。
 *
 * 编号取数说明：MAX 查询故意不过滤 del_flag —— 已删除单子的编号也不许复用；
 * FOR UPDATE 走当前已提交数据，避免事务快照里读到旧的最大号。
 */
@Mapper
public interface StockCheckMapper extends BaseMapper<StockCheckPO> {

    @Select("SELECT MAX(check_no) FROM t_stock_check WHERE check_no LIKE CONCAT(#{prefix}, '%') FOR UPDATE")
    String maxCheckNo(@Param("prefix") String prefix);
}
