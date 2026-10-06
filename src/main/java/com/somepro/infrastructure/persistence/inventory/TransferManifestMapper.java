package com.somepro.infrastructure.persistence.inventory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.inventory.po.TransferManifestPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 转移联单的 MyBatis-Plus Mapper（基础设施层）。阻塞 JDBC，只能在 boundedElastic 线程上调用。
 */
@Mapper
public interface TransferManifestMapper extends BaseMapper<TransferManifestPO> {
}
