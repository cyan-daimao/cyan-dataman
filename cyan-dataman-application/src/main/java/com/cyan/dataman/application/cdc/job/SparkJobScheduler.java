package com.cyan.dataman.application.cdc.job;

import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.cdc.repository.CdcSparkJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Spark 任务调度器
 * <p>
 * 每 60s 轮询 enabled=true 且配置了 cron 表达式的 Spark Job，
 * 匹配当前时间触发执行。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class SparkJobScheduler {

    private final CdcSparkJobRepository cdcSparkJobRepository;
    private final CdcConfigRepository cdcConfigRepository;
    private final SparkJobExecutor sparkJobExecutor;

    public SparkJobScheduler(CdcSparkJobRepository cdcSparkJobRepository,
                             CdcConfigRepository cdcConfigRepository,
                             SparkJobExecutor sparkJobExecutor) {
        this.cdcSparkJobRepository = cdcSparkJobRepository;
        this.cdcConfigRepository = cdcConfigRepository;
        this.sparkJobExecutor = sparkJobExecutor;
    }

    /**
     * 每 60s 轮询 enabled=true 的 Spark Job，匹配 cron 表达式触发执行
     */
    @Scheduled(fixedDelay = 60000)
    public void scheduleSparkJobs() {
        List<CdcSparkJob> enabledJobs = cdcSparkJobRepository.findAllEnabled();
        if (enabledJobs.isEmpty()) {
            return;
        }

        for (CdcSparkJob job : enabledJobs) {
            try {
                if (!shouldTrigger(job)) {
                    continue;
                }

                CdcConfig config = cdcConfigRepository.findById(job.getCdcConfigId());
                if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
                    log.debug("Spark Job {} 关联的 CDC 配置未启用，跳过", job.getId());
                    continue;
                }

                log.info("Cron 触发 Spark 任务: sparkJobId={}, cdcConfigId={}, syncMode={}, cron={}",
                        job.getId(), job.getCdcConfigId(), job.getSyncMode(), job.getCronExpression());
                sparkJobExecutor.executeSparkJob(job, config);
            } catch (Exception e) {
                log.error("调度 Spark 任务异常: sparkJobId={}, error={}", job.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 判断是否应该触发执行
     * <p>
     * 解析 cron 表达式，计算下一次执行时间，判断是否在当前分钟内。
     */
    private boolean shouldTrigger(CdcSparkJob job) {
        if (job.getCronExpression() == null || job.getCronExpression().isBlank()) {
            return false;
        }

        try {
            CronExpression cron = CronExpression.parse(job.getCronExpression());
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime nextExecution = cron.next(now.minusMinutes(1));
            return nextExecution != null && !nextExecution.isAfter(now.plusMinutes(1));
        } catch (Exception e) {
            log.warn("解析 cron 表达式失败: {}, expression={}", e.getMessage(), job.getCronExpression());
            return false;
        }
    }
}
