package com.cyan.dataman.application.metadata.quality.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据质量规则BO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityRuleBO {

    /**
     * 主键
     */
    private String id;

    /**
     * 元数据表ID
     */
    private String tableId;

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
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
