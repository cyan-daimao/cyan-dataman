package com.cyan.dataman.infra.persistence.metadata.quality.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityRuleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 元数据质量规则Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface MetadataQualityRuleMapper extends BaseMapper<MetadataQualityRuleDO> {
}
