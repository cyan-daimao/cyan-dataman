package com.cyan.dataman.infra.persistence.metadata.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataColumnDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元数据表字段Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataColumnMapper extends BaseMapper<MetadataColumnDO> {
}
