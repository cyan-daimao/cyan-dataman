package com.cyan.dataman.adapter.metadata.quality.http.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据质量告警DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityAlertDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 元数据表ID
     */
    private String tableId;

    /**
     * 运行ID
     */
    private String runId;

    /**
     * 结果ID
     */
    private String resultId;

    /**
     * 规则ID
     */
    private String ruleId;

    /**
     * 告警标题
     */
    private String title;

    /**
     * 告警内容
     */
    private String message;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 告警状态
     */
    private String status;

    /**
     * 关闭人
     */
    private String closedBy;

    /**
     * 关闭时间
     */
    private LocalDateTime closedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
