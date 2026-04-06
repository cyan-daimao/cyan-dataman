package com.cyan.dataman.infra.persistence.cdc.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataman.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC Spark 任务实例表
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("cdc_spark_task")
public class CdcSparkTaskDO {

    /**
     * 主键
     */
    @TableId("id")
    private String id;

    /**
     * Spark 作业配置 ID
     */
    @TableField("spark_job_id")
    private String sparkJobId;

    /**
     * CDC 配置 ID
     */
    @TableField("cdc_config_id")
    private String cdcConfigId;

    /**
     * 任务状态
     */
    @TableField("status")
    private JobStatus status;

    /**
     * Spark 应用 ID
     */
    @TableField("application_id")
    private String applicationId;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 运行时长（秒）
     */
    @TableField("duration_seconds")
    private Long durationSeconds;

    /**
     * 影响行数
     */
    @TableField("rows_affected")
    private Long rowsAffected;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableField("deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
