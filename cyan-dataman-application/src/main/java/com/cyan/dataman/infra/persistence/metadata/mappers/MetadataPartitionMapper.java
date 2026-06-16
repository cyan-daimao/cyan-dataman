package com.cyan.dataman.infra.persistence.metadata.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataPartitionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元数据表分区 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataPartitionMapper extends BaseMapper<MetadataPartitionDO> {
}
