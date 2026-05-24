package com.cyan.dataman.adapter.metadata.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * AI 关联推荐响应
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class AiRelationSuggestionDTO {

    /**
     * 源表 catalog
     */
    private String sourceCatalog;

    /**
     * 源表 schema
     */
    private String sourceSchema;

    /**
     * 源表名
     */
    private String sourceTable;

    /**
     * 源表注释
     */
    private String sourceTableComment;

    /**
     * 源表字段
     */
    private String sourceColumn;

    /**
     * 源表字段选项
     */
    private List<AiRelationColumnDTO> sourceColumns;

    /**
     * 目标表 catalog
     */
    private String targetCatalog;

    /**
     * 目标表 schema
     */
    private String targetSchema;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 目标表注释
     */
    private String targetTableComment;

    /**
     * 目标表字段
     */
    private String targetColumn;

    /**
     * 目标表字段选项
     */
    private List<AiRelationColumnDTO> targetColumns;

    /**
     * JOIN 类型
     */
    private String joinType;

    /**
     * 置信度
     */
    private Double confidence;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 关系描述
     */
    private String description;
}
