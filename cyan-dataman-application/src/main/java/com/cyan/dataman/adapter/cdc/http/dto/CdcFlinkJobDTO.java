package com.cyan.dataman.adapter.cdc.http.dto;

import com.cyan.dataman.enums.JobStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC Flink 作业配置 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class CdcFlinkJobDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 数据源名称（一数据源一作业）
     */
    private String dsName;

    /**
     * Flink 的 job id
     */
    private String flinkJobId;

    /**
     * 日志路径
     */
    private String logPath;

    /**
     * Flink SQL 文本
     */
    private String flinkSql;

    /**
     * Flink 任务状态
     */
    private JobStatus status;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
