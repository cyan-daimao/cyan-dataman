package com.cyan.dataman.application.cdc.bo;

import com.cyan.dataman.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC Flink 作业配置业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CdcFlinkJobBO {

    /**
     * 主键
     */
    private String id;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据库名
     */
    private String dbName;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 主题编码
     */
    private String subjectCode;

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
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
