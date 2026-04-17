package com.cyan.dataman.domain.cdc.repository;

import com.cyan.dataman.domain.cdc.CdcSparkTask;
import com.cyan.dataman.enums.JobStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CDC Spark 任务实例仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface CdcSparkTaskRepository {

    /**
     * 保存
     */
    CdcSparkTask save(CdcSparkTask task);

    /**
     * 根据 ID 查找
     */
    CdcSparkTask findById(String id);

    /**
     * 根据 Spark 作业 ID 查找任务列表
     */
    List<CdcSparkTask> findBySparkJobId(String sparkJobId);

    /**
     * 根据 CDC 配置 ID 查找任务列表
     */
    List<CdcSparkTask> findByCdcConfigId(String cdcConfigId);

    /**
     * 查找运行中的任务
     */
    List<CdcSparkTask> findRunningTasks(LocalDateTime startTime);

    /**
     * 根据状态查找任务
     */
    List<CdcSparkTask> findByStatus(JobStatus status);

    /**
     * 根据 CDC 配置 ID 查找运行中的任务
     */
    List<CdcSparkTask> findRunningByCdcConfigId(String cdcConfigId);
}
