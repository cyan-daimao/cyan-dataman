package com.cyan.dataman.application.metadata.quality.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 元数据质量规则模板BO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityRuleTemplateBO {

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
