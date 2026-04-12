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
     * CDC 配置 ID
     */
    private String cdcConfigId;

    /**
     * Flink 的 job id
     */
    private String flinkJobId;

    /**
     * 日志路径
     */
    private String logPath;

    /**
     * Flink SQL 模板
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
        Assert.notBlank(this.cdcConfigId, new SilentException("CDC 配置 ID 不能为空"));
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
        this.status = this.status==null?JobStatus.PENDING:this.status;
        this.logPath = StrUtils.isBlank(this.logPath)?"":this.logPath;
        this.flinkSql = StrUtils.isBlank(this.flinkSql)?"":this.flinkSql;
        this.errorMessage = StrUtils.isBlank(this.errorMessage)?"":this.errorMessage;
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
