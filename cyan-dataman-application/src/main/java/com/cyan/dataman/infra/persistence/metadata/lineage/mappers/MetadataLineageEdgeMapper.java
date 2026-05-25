package com.cyan.dataman.infra.persistence.metadata.lineage.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.lineage.dos.MetadataLineageEdgeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元数据血缘边 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataLineageEdgeMapper extends BaseMapper<MetadataLineageEdgeDO> {
}
