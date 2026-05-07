package com.cyan.dataman.infra.persistence.metadata.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableRelationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元数据表关系Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataTableRelationMapper extends BaseMapper<MetadataTableRelationDO> {
}
