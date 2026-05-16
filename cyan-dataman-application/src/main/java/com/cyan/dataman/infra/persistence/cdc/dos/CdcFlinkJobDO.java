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
 * CDC Flink 作业配置表
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("cdc_flink_job")
public class CdcFlinkJobDO {

    /**
     * 主键
     */
    @TableId("id")
    private Long id;

    /**
     * 数据源名称（一数据源一作业）
     */
    @TableField("ds_name")
    private String dsName;

    /**
     * flink的job id
     */
    @TableField("flink_job_id")
    private String flinkJobId;

    /**
     * 日志路径
     */
    @TableField("log_path")
    private String logPath;

    /**
     * flink SQL 文本
     */
    @TableField("flink_sql")
    private String flinkSql;

    /**
     * Flink 任务状态
     */
    @TableField("status")
    private JobStatus status;

    /**
     * 是否启用调度
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 修改人
     */
    @TableField("update_by")
    private String updateBy;

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
