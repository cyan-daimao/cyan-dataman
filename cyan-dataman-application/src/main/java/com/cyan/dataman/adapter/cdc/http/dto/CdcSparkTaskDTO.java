package com.cyan.dataman.adapter.cdc.http.dto;

import com.cyan.dataman.enums.JobStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC Spark 任务实例 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class CdcSparkTaskDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * Spark 作业配置 ID
     */
    private String sparkJobId;

    /**
     * CDC 配置 ID
     */
    private String cdcConfigId;

    /**
     * 任务状态
     */
    private JobStatus status;

    /**
     * Spark 应用 ID
     */
    private String applicationId;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 运行时长（秒）
     */
    private Long durationSeconds;

    /**
     * 影响行数
     */
    private Long rowsAffected;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
