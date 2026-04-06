package com.cyan.dataman.domain.cdc;

import com.cyan.dataman.domain.cdc.repository.CdcSparkTaskRepository;
import com.cyan.dataman.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * CDC Spark 任务实例
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CdcSparkTask {

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

    /**
     * 逻辑删除
     */
    private LocalDateTime deletedAt;

    /**
     * 开始执行
     */
    public CdcSparkTask start(CdcSparkTaskRepository repository, String applicationId) {
        this.status = JobStatus.RUNNING;
        this.startTime = LocalDateTime.now();
        this.applicationId = applicationId;
        this.updatedAt = LocalDateTime.now();
        return repository.save(this);
    }

    /**
     * 执行成功
     */
    public CdcSparkTask success(CdcSparkTaskRepository repository, Long rowsAffected) {
        this.status = JobStatus.SUCCESS;
        this.endTime = LocalDateTime.now();
        this.rowsAffected = rowsAffected;
        this.durationSeconds = calculateDuration();
        this.updatedAt = LocalDateTime.now();
        return repository.save(this);
    }

    /**
     * 执行失败
     */
    public CdcSparkTask fail(CdcSparkTaskRepository repository, String errorMessage) {
        this.status = JobStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.durationSeconds = calculateDuration();
        this.updatedAt = LocalDateTime.now();
        return repository.save(this);
    }

    /**
     * 停止
     */
    public CdcSparkTask stop(CdcSparkTaskRepository repository) {
        this.status = JobStatus.STOPPED;
        this.endTime = LocalDateTime.now();
        this.durationSeconds = calculateDuration();
        this.updatedAt = LocalDateTime.now();
        return repository.save(this);
    }

    /**
     * 计算运行时长
     */
    private Long calculateDuration() {
        if (this.startTime == null) {
            return 0L;
        }
        LocalDateTime end = this.endTime != null ? this.endTime : LocalDateTime.now();
        return Duration.between(this.startTime, end).getSeconds();
    }

    /**
     * 保存
     */
    public CdcSparkTask save(CdcSparkTaskRepository repository) {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        return repository.save(this);
    }
}
