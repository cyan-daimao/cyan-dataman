package com.cyan.dataman.application.metadata.quality.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 元数据质量规则推荐候选BO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityRuleSuggestionBO {

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 质量维度
     */
    private String dimension;

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 规则配置JSON
     */
    private String configJson;

    /**
     * 过滤条件SQL
     */
    private String filterSql;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 推荐原因
     */
    private String reason;

    /**
     * 置信度
     */
    private BigDecimal confidence;

    /**
     * 是否已有重复规则
     */
    private Boolean exists;
}
