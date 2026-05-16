package com.cyan.dataman.domain.cdc;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataman.domain.cdc.repository.CdcFlinkJobRepository;
import com.cyan.dataman.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC Flink 作业配置实体
 * <p>
 * 一数据源对应一 Flink 作业，通过 dsName 关联。
 * 作业内通过 Kafka topic pattern 消费该数据源下所有 CDC 表的数据。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CdcFlinkJob {

    /**
     * 主键（雪花算法自动生成）
     */
    private Long id;

    /**
     * 数据源名称（一数据源一作业）
     */
    private String dsName;

    /**
     * 主题编码（ODS 表前缀）
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

    /**
     * 逻辑删除
     */
    private LocalDateTime deletedAt;

    /**
     * 验证
     */
    private void validate() {
        Assert.notBlank(this.dsName, new SilentException("数据源名称不能为空"));
    }

    /**
     * 保存
     */
    public CdcFlinkJob save(CdcFlinkJobRepository repository) {
        validate();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = false;
        }
        this.status = this.status == null ? JobStatus.PENDING : this.status;
        this.logPath = StrUtils.isBlank(this.logPath) ? "" : this.logPath;
        this.flinkSql = StrUtils.isBlank(this.flinkSql) ? "" : this.flinkSql;
        this.errorMessage = StrUtils.isBlank(this.errorMessage) ? "" : this.errorMessage;
        return repository.save(this);
    }

    /**
     * 更新
     */
    public CdcFlinkJob update(CdcFlinkJobRepository repository) {
        validate();
        this.updatedAt = LocalDateTime.now();
        return repository.update(this);
    }

    /**
     * 删除
     */
    public void delete(CdcFlinkJobRepository repository) {
        repository.deleteById(this.id);
    }

    /**
     * 停止任务
     */
    public void stop(CdcFlinkJobRepository repository) {
        this.status = JobStatus.STOPPED;
        this.updatedAt = LocalDateTime.now();
        repository.update(this);
    }
}
