package com.cyan.dataman.infra.persistence.metadata.quality.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityResultDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元数据质量结果Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataQualityResultMapper extends BaseMapper<MetadataQualityResultDO> {
}
