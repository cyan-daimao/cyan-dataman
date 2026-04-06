package com.cyan.dataman.application.cdc.job;

import com.cyan.dataman.application.cdc.event.SparkJobEvent;
import com.cyan.dataman.domain.cdc.CdcSparkTask;
import com.cyan.dataman.domain.cdc.repository.CdcSparkTaskRepository;
import com.cyan.dataman.enums.JobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spark 任务执行器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class SparkJobExecutor {

    private final CdcSparkTaskRepository cdcSparkTaskRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SparkJobExecutor(CdcSparkTaskRepository cdcSparkTaskRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.cdcSparkTaskRepository = cdcSparkTaskRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 监听 Spark 任务提交事件
     */
    @EventListener
    @Async
    public void handleSubmitEvent(SparkJobEvent event) {
        if (event.getType() != SparkJobEvent.EventType.SUBMIT) {
            return;
        }

        log.info("提交 Spark 任务，cdcConfigId: {}, sparkSql: {}", event.getCdcConfigId(), event.getSparkSql());

        // 创建任务实例
        CdcSparkTask task = new CdcSparkTask();
        task.setId(UUID.randomUUID().toString());
        task.setSparkJobId(event.getSparkJobId());
        task.setCdcConfigId(event.getCdcConfigId());
        task.setStatus(JobStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        task = cdcSparkTaskRepository.save(task);

        // TODO: 实际场景中这里应该提交到 Spark 集群
        // 模拟执行 Spark SQL
        executeSparkTask(task, event.getSparkSql());
    }

    /**
     * 监听 Spark 任务开始事件
     */
    @EventListener
    @Async
    public void handleStartEvent(SparkJobEvent event) {
        if (event.getType() != SparkJobEvent.EventType.START) {
            return;
        }

        log.info("开启 Spark 任务，applicationId: {}", event.getApplicationId());

        CdcSparkTask task = cdcSparkTaskRepository.findById(event.getSparkJobId());
        if (task != null) {
            task.start(cdcSparkTaskRepository, event.getApplicationId());
        }
    }

    /**
     * 监听 Spark 任务停止事件
     */
    @EventListener
    @Async
    public void handleStopEvent(SparkJobEvent event) {
        if (event.getType() != SparkJobEvent.EventType.STOP) {
            return;
        }

        log.info("关闭 Spark 任务，applicationId: {}", event.getApplicationId());

        CdcSparkTask task = cdcSparkTaskRepository.findById(event.getSparkJobId());
        if (task != null && task.getStatus() == JobStatus.RUNNING) {
            task.stop(cdcSparkTaskRepository);
        }
    }

    /**
     * 执行 Spark 任务（模拟实现）
     */
    private void executeSparkTask(CdcSparkTask task, String sparkSql) {
        log.info("执行 Spark SQL: {}", sparkSql);

        try {
            // 模拟任务执行
            // 实际场景应该调用 Spark submit 或 Spark API

            // 更新任务状态为运行中
            task.start(cdcSparkTaskRepository, "spark-app-" + System.currentTimeMillis());

            // 模拟执行时间
            Thread.sleep(5000);

            // 模拟执行成功
            long rowsAffected = 1000L; // 模拟影响的行数
            task.success(cdcSparkTaskRepository, rowsAffected);

            log.info("Spark 任务执行成功，影响行数：{}", rowsAffected);

        } catch (Exception e) {
            log.error("Spark 任务执行失败", e);
            task.fail(cdcSparkTaskRepository, e.getMessage());
        }
    }

    /**
     * 提交 Spark SQL 任务
     *
     * @param sparkJobId Spark 作业配置 ID
     * @param cdcConfigId CDC 配置 ID
     * @param sparkSql Spark SQL
     */
    public void submitSparkJob(String sparkJobId, String cdcConfigId, String sparkSql) {
        SparkJobEvent event = new SparkJobEvent();
        event.setType(SparkJobEvent.EventType.SUBMIT);
        event.setSparkJobId(sparkJobId);
        event.setCdcConfigId(cdcConfigId);
        event.setSparkSql(sparkSql);

        // 发布事件
        eventPublisher.publishEvent(event);
        log.info("已提交 Spark 任务事件，cdcConfigId: {}", cdcConfigId);
    }
}
