package com.cyan.dataman.adapter.metadata.quality.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityAlertDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityResultDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRuleDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRuleRequestDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRuleTemplateDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRunDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualitySummaryDTO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityAlertBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityResultBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleTemplateBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRunBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualitySummaryBO;
import com.cyan.dataman.application.metadata.quality.cmd.MetadataQualityRuleCmd;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据质量适配层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataQualityAdapterConvert {

    MetadataQualityAdapterConvert INSTANCE = Mappers.getMapper(MetadataQualityAdapterConvert.class);

    /**
     * 转换规则命令
     */
    MetadataQualityRuleCmd toRuleCmd(MetadataQualityRuleRequestDTO dto);

    /**
     * 转换规则模板DTO列表
     */
    List<MetadataQualityRuleTemplateDTO> toRuleTemplateDTOList(List<MetadataQualityRuleTemplateBO> templates);

    /**
     * 转换质量汇总DTO
     */
    MetadataQualitySummaryDTO toSummaryDTO(MetadataQualitySummaryBO summary);

    /**
     * 转换规则DTO
     */
    MetadataQualityRuleDTO toRuleDTO(MetadataQualityRuleBO rule);

    /**
     * 转换规则DTO列表
     */
    List<MetadataQualityRuleDTO> toRuleDTOList(List<MetadataQualityRuleBO> rules);

    /**
     * 转换结果DTO
     */
    MetadataQualityResultDTO toResultDTO(MetadataQualityResultBO result);

    /**
     * 转换运行DTO
     */
    MetadataQualityRunDTO toRunDTO(MetadataQualityRunBO run);

    /**
     * 转换运行DTO列表
     */
    List<MetadataQualityRunDTO> toRunDTOList(List<MetadataQualityRunBO> runs);

    /**
     * 转换告警DTO
     */
    MetadataQualityAlertDTO toAlertDTO(MetadataQualityAlertBO alert);

    /**
     * 转换告警DTO列表
     */
    List<MetadataQualityAlertDTO> toAlertDTOList(List<MetadataQualityAlertBO> alerts);
}
