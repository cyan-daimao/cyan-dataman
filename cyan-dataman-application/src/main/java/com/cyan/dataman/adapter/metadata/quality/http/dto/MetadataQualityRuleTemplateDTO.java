package com.cyan.dataman.adapter.metadata.quality.http.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 元数据质量规则模板DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityRuleTemplateDTO {

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 质量维度
     */
    private String dimension;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则说明
     */
    private String description;

    /**
     * 是否字段级规则
     */
    private Boolean columnRequired;

    /**
     * 示例配置JSON
     */
    private String configExample;
}
