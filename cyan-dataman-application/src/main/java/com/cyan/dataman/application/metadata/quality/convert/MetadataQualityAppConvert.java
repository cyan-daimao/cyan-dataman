package com.cyan.dataman.application.metadata.quality.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityAlertBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityResultBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRunBO;
import com.cyan.dataman.application.metadata.quality.cmd.MetadataQualityRuleCmd;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityAlert;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityResult;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityRule;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据质量应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataQualityAppConvert {

    MetadataQualityAppConvert INSTANCE = Mappers.getMapper(MetadataQualityAppConvert.class);

    /**
     * 转换规则领域对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tableId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    MetadataQualityRule toRule(MetadataQualityRuleCmd cmd);

    /**
     * 转换规则BO
     */
    MetadataQualityRuleBO toRuleBO(MetadataQualityRule rule);

    /**
     * 转换规则BO列表
     */
    List<MetadataQualityRuleBO> toRuleBOList(List<MetadataQualityRule> rules);

    /**
     * 转换运行BO
     */
    @Mapping(target = "results", ignore = true)
    MetadataQualityRunBO toRunBO(MetadataQualityRun run);

    /**
     * 转换运行BO列表
     */
    List<MetadataQualityRunBO> toRunBOList(List<MetadataQualityRun> runs);

    /**
     * 转换结果BO
     */
    MetadataQualityResultBO toResultBO(MetadataQualityResult result);

    /**
     * 转换结果BO列表
     */
    List<MetadataQualityResultBO> toResultBOList(List<MetadataQualityResult> results);

    /**
     * 转换告警BO
     */
    MetadataQualityAlertBO toAlertBO(MetadataQualityAlert alert);

    /**
     * 转换告警BO列表
     */
    List<MetadataQualityAlertBO> toAlertBOList(List<MetadataQualityAlert> alerts);
}
