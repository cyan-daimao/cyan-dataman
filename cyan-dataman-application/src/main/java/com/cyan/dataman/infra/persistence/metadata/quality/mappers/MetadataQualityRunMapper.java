package com.cyan.dataman.infra.persistence.metadata.quality.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityRunDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元数据质量运行Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataQualityRunMapper extends BaseMapper<MetadataQualityRunDO> {
}
