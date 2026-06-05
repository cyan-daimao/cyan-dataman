package com.cyan.dataman.adapter.metadata.quality.http.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据质量结果DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityResultDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 运行ID
     */
    private String runId;

    /**
     * 规则ID
     */
    private String ruleId;

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
     * 检查状态
     */
    private String status;

    /**
     * 实际值
     */
    private String actualValue;

    /**
     * 期望值
     */
    private String expectedValue;

    /**
     * 总行数
     */
    private Long totalCount;

    /**
     * 失败行数
     */
    private Long failCount;

    /**
     * 样本SQL
     */
    private String sampleSql;

    /**
     * 明细JSON
     */
    private String detailJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
