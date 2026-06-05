package com.cyan.dataman.infra.persistence.metadata.quality.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityAlertDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元数据质量告警Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataQualityAlertMapper extends BaseMapper<MetadataQualityAlertDO> {
}
