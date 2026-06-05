package com.cyan.dataman.adapter.metadata.quality.http.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 元数据质量汇总DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualitySummaryDTO {

    /**
     * 元数据表ID
     */
    private String tableId;

    /**
     * 最新质量分数
     */
    private BigDecimal score;

    /**
     * 最新运行ID
     */
    private String latestRunId;

    /**
     * 最新运行状态
     */
    private String latestRunStatus;

    /**
     * 最近运行时间
     */
    private LocalDateTime latestRunTime;

    /**
     * 通过规则数
     */
    private Integer passCount;

    /**
     * 警告规则数
     */
    private Integer warnCount;

    /**
     * 失败规则数
     */
    private Integer failCount;

    /**
     * 规则总数
     */
    private Integer ruleCount;

    /**
     * 启用规则数
     */
    private Integer enabledRuleCount;

    /**
     * 未关闭告警数
     */
    private Integer openAlertCount;
}
