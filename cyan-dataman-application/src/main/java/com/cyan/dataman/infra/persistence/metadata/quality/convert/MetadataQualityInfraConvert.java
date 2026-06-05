package com.cyan.dataman.infra.persistence.metadata.quality.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityAlert;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityResult;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityRule;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityRun;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityAlertDO;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityResultDO;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityRuleDO;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityRunDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据质量基础设施层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataQualityInfraConvert {

    MetadataQualityInfraConvert INSTANCE = Mappers.getMapper(MetadataQualityInfraConvert.class);

    /**
     * 转换规则领域对象
     */
    MetadataQualityRule toRule(MetadataQualityRuleDO ruleDO);

    /**
     * 转换规则DO
     */
    MetadataQualityRuleDO toRuleDO(MetadataQualityRule rule);

    /**
     * 转换规则列表
     */
    List<MetadataQualityRule> toRuleList(List<MetadataQualityRuleDO> ruleDOList);

    /**
     * 转换运行领域对象
     */
    MetadataQualityRun toRun(MetadataQualityRunDO runDO);

    /**
     * 转换运行DO
     */
    MetadataQualityRunDO toRunDO(MetadataQualityRun run);

    /**
     * 转换运行列表
     */
    List<MetadataQualityRun> toRunList(List<MetadataQualityRunDO> runDOList);

    /**
     * 转换结果领域对象
     */
    MetadataQualityResult toResult(MetadataQualityResultDO resultDO);

    /**
     * 转换结果DO
     */
    MetadataQualityResultDO toResultDO(MetadataQualityResult result);

    /**
     * 转换结果列表
     */
    List<MetadataQualityResult> toResultList(List<MetadataQualityResultDO> resultDOList);

    /**
     * 转换结果DO列表
     */
    List<MetadataQualityResultDO> toResultDOList(List<MetadataQualityResult> resultList);

    /**
     * 转换告警领域对象
     */
    MetadataQualityAlert toAlert(MetadataQualityAlertDO alertDO);

    /**
     * 转换告警DO
     */
    MetadataQualityAlertDO toAlertDO(MetadataQualityAlert alert);

    /**
     * 转换告警列表
     */
    List<MetadataQualityAlert> toAlertList(List<MetadataQualityAlertDO> alertDOList);
}
