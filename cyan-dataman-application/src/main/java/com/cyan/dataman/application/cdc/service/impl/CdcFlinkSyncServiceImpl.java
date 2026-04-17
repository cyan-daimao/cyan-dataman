package com.cyan.dataman.application.cdc.service.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.cdc.service.CdcFlinkSyncService;
import com.cyan.dataman.application.cdc.sink.DebeziumToIcebergProcessFunction;
import com.cyan.dataman.application.cdc.sink.IcebergBatchSink;
import com.cyan.dataman.application.cdc.sink.IcebergWriteRecord;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcFlinkJob;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.cdc.repository.CdcFlinkJobRepository;
import com.cyan.dataman.enums.JobStatus;
import com.cyan.dataman.enums.SyncTool;
import com.cyan.dataman.infra.config.FlinkConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CDC Flink 同步服务实现
 * <p>
 * 架构：一数据源一 Flink 作业，通过 topic pattern 自动匹配该 connector 下所有 topic，
 * ProcessFunction 定时从数据库刷新路由配置，实现动态增减表。
 * <p>
 * 支持本地模式和远程模式：
 * - local: 在 Spring Boot 进程内本地运行 Flink 作业
 * - remote: 通过 RemoteStreamEnvironment 提交到远端 Flink 集群，Spring Boot 重启不影响远程集群
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class CdcFlinkSyncServiceImpl implements CdcFlinkSyncService {

    @Value("${flink.rest.url:localhost:6123}")
    private String flinkRestUrl;

    @Value("${kafka.url:kafka:9092}")
    private String kafkaBootstrapServers;

    @Value("${iceberg.uri:http://iceberg-rest.cyan.com/iceberg}")
    private String icebergRestUri;

    @Value("${rustfs.endpoint:http://10.0.0.2:9000}")
    private String rustfsEndpoint;

    @Value("${rustfs.accessKey:rustfsadmin}")
    private String rustfsAccessKey;

    @Value("${rustfs.secretKey:rustfsadmin}")
    private String rustfsSecretKey;

    /**
     * 数据源 JDBC 连接信息（供 Flink ProcessFunction 定时查库用）
     */
    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String jdbcUsername;

    @Value("${spring.datasource.password}")
    private String jdbcPassword;

    /**
     * 数据源名称 -> 运行中的 Flink 作业 ID
     */
    private final Map<String, String> dsNameToFlinkJobId = new ConcurrentHashMap<>();

    private final CdcConfigRepository cdcConfigRepository;
    private final CdcFlinkJobRepository cdcFlinkJobRepository;
    private final FlinkConfig flinkConfig;
    private final HttpClient httpClient;

    public CdcFlinkSyncServiceImpl(FlinkConfig flinkConfig,
                                   CdcConfigRepository cdcConfigRepository,
                                   CdcFlinkJobRepository cdcFlinkJobRepository) {
        this.flinkConfig = flinkConfig;
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcFlinkJobRepository = cdcFlinkJobRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void startFlinkSyncJob() {
        // 从数据库恢复已运行作业的映射
        recoverRunningJobs();

        // 对没有运行中作业的数据源提交新作业
        List<CdcConfig> enabledConfigs = getEnabledFlinkConfigs();
        Map<String, List<CdcConfig>> configsByDsName = new HashMap<>();
        for (CdcConfig config : enabledConfigs) {
            configsByDsName.computeIfAbsent(config.getDsName(), k -> new ArrayList<>()).add(config);
        }

        for (Map.Entry<String, List<CdcConfig>> entry : configsByDsName.entrySet()) {
            String dsName = entry.getKey();
            if (dsNameToFlinkJobId.containsKey(dsName)) {
                log.info("数据源 {} 已有运行中的 Flink 作业，跳过提交", dsName);
                continue;
            }
            // 取该数据源下任意一个 config 来提交作业（作业内 ProcessFunction 会自动感知所有 enabled 的表）
            CdcConfig config = entry.getValue().getFirst();
            CompletableFuture.runAsync(() -> submitFlinkJob(dsName, config));
        }
    }

    @Override
    public void stopFlinkSyncJob() {
        for (Map.Entry<String, String> entry : dsNameToFlinkJobId.entrySet()) {
            cancelFlinkJob(entry.getValue());
        }
        log.info("已停止所有 Flink CDC 同步作业");
    }

    @Override
    public void enableCdcSync(String cdcConfigId) {
        CdcConfig config = cdcConfigRepository.findById(cdcConfigId);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));
        Assert.isTrue(SyncTool.FLINK.equals(config.getSyncTool()),
                new SilentException("该 CDC 配置不是 FLINK 类型"));

        if (!Boolean.TRUE.equals(config.getEnabled())) {
            config.toggle(cdcConfigRepository, true);
        }

        String dsName = config.getDsName();

        // 只有该数据源没有运行中的 Flink 作业时才提交新作业
        // 已有作业时，ProcessFunction 会在下一个刷新周期自动感知新表
        if (!dsNameToFlinkJobId.containsKey(dsName)) {
            CompletableFuture.runAsync(() -> submitFlinkJob(dsName, config));
        } else {
            log.info("数据源 {} 已有运行中的 Flink 作业，新表将由 ProcessFunction 自动感知, table={}.{}",
                    dsName, config.getDbName(), config.getTableName());
        }
    }

    @Override
    public void disableCdcSync(String cdcConfigId) {
        CdcConfig config = cdcConfigRepository.findById(cdcConfigId);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        if (Boolean.TRUE.equals(config.getEnabled())) {
            config.toggle(cdcConfigRepository, false);
        }

        String dsName = config.getDsName();
        log.info("已禁用 CDC 同步, dsName={}, table={}.{}，Flink 作业将在刷新周期内自动跳过该表",
                dsName, config.getDbName(), config.getTableName());

        // 检查该数据源下是否还有启用的表，没有则停止 Flink 作业
        List<CdcConfig> remainingEnabled = cdcConfigRepository.list(
                new CdcConfigListQuery().setDsName(dsName)
                        .setEnabled(true)
                        .setSyncTool(SyncTool.FLINK));

        if (remainingEnabled.isEmpty()) {
            String flinkJobId = dsNameToFlinkJobId.remove(dsName);
            if (flinkJobId != null) {
                cancelFlinkJob(flinkJobId);
                log.info("数据源 {} 没有启用的表，已停止 Flink 作业", dsName);
            }
        }
    }

    @Override
    public void cancelFlinkJob(String flinkJobId) {
        CdcFlinkJob flinkJob = cdcFlinkJobRepository.findByFlinkJobId(flinkJobId);
        if (flinkJob == null) {
            log.warn("Flink 作业不存在，flinkJobId: {}", flinkJobId);
            return;
        }

        if ("remote".equalsIgnoreCase(flinkConfig.getFlinkMode())) {
            cancelRemoteFlinkJob(flinkJobId);
        }

        flinkJob.setStatus(JobStatus.STOPPED);
        flinkJob.setUpdatedAt(LocalDateTime.now());
        flinkJob.update(cdcFlinkJobRepository);

        dsNameToFlinkJobId.entrySet().removeIf(entry -> entry.getValue().equals(flinkJobId));
        log.info("已取消 Flink CDC 作业，flinkJobId: {}", flinkJobId);
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    public void refreshSyncStatus() {
        if ("remote".equalsIgnoreCase(flinkConfig.getFlinkMode())) {
            List<CdcFlinkJob> runningJobs = cdcFlinkJobRepository.findAllRunning();

            for (CdcFlinkJob job : runningJobs) {
                try {
                    String jobUrl = "http://" + flinkRestUrl + "/jobs/" + job.getFlinkJobId();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(jobUrl))
                            .GET()
                            .timeout(Duration.ofSeconds(5))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 404) {
                        job.setStatus(JobStatus.STOPPED);
                        job.setUpdatedAt(LocalDateTime.now());
                        job.update(cdcFlinkJobRepository);
                        dsNameToFlinkJobId.entrySet().removeIf(e -> e.getValue().equals(job.getFlinkJobId()));
                        log.info("Flink 作业已结束，更新状态，flinkJobId: {}", job.getFlinkJobId());
                    }
                } catch (Exception e) {
                    log.debug("检查 Flink 作业状态失败，flinkJobId: {}, error: {}", job.getFlinkJobId(), e.getMessage());
                }
            }
        }
    }

    // ==================== 作业提交 ====================

    /**
     * 构建 DAG 并提交 Flink 作业
     * <p>
     * 每次调用创建新的 StreamExecutionEnvironment，不复用。
     * Kafka Source 使用 topic pattern 自动匹配该 connector 下所有 topic。
     * ProcessFunction 定时从数据库刷新路由配置，支持动态增减表。
     */
    private void submitFlinkJob(String dsName, CdcConfig config) {
        StreamExecutionEnvironment env = flinkConfig.createStreamExecutionEnvironment();

        // 使用 topic pattern 匹配该数据源 connector 下所有 topic
        // 格式: cdc-{dsName}.*，例如 cdc-mysql-x99.*
        // Debezium topic 格式为 {connectorName}.{dbName}.{tableName}
        String topicPattern = "cdc-" + dsName + ".*";

        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaBootstrapServers)
                .setTopicPattern(java.util.regex.Pattern.compile(topicPattern))
                .setGroupId("flink-cdc-consumer-" + dsName)
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setStartingOffsets(OffsetsInitializer.committedOffsets(
                        org.apache.kafka.clients.consumer.OffsetResetStrategy.EARLIEST))
                .build();

        DataStream<String> rawStream = env.fromSource(
                        kafkaSource,
                        WatermarkStrategy.noWatermarks(),
                        "Kafka CDC Source - " + dsName)
                .uid("kafka-source-" + dsName);

        // 解析 Debezium 消息并路由到目标 Iceberg 表（定时从数据库刷新配置）
        DataStream<IcebergWriteRecord> recordStream = rawStream
                .process(new DebeziumToIcebergProcessFunction(
                        dsName,
                        icebergRestUri,
                        rustfsEndpoint,
                        rustfsAccessKey,
                        rustfsSecretKey,
                        jdbcUrl,
                        jdbcUsername,
                        jdbcPassword))
                .uid("cdc-parse-" + dsName)
                .name("CDC Parse - " + dsName);

        // 批量写入 Iceberg（利用 Checkpoint 批量提交）
        recordStream
                .process(new IcebergBatchSink(
                        icebergRestUri,
                        rustfsEndpoint,
                        rustfsAccessKey,
                        rustfsSecretKey,
                        128 * 1024 * 1024L))
                .name("Iceberg Sink - " + dsName)
                .uid("iceberg-sink-" + dsName);

        try {
            var result = env.execute("Flink CDC Sync - " + dsName);
            String realJobId = result.getJobID().toString();

            CdcFlinkJob flinkJob = new CdcFlinkJob()
                    .setFlinkJobId(realJobId)
                    .setCdcConfigId(config.getId())
                    .setEnabled(true)
                    .setStatus(JobStatus.RUNNING)
                    .setCreatedAt(LocalDateTime.now())
                    .setUpdatedAt(LocalDateTime.now())
                    .setCreateBy(config.getCreateBy())
                    .setUpdateBy(config.getUpdateBy());
            flinkJob.save(cdcFlinkJobRepository);

            dsNameToFlinkJobId.put(dsName, realJobId);
            log.info("Flink CDC 作业已提交, mode={}, dsName={}, flinkJobId={}, topicPattern={}",
                    flinkConfig.getFlinkMode(), dsName, realJobId, topicPattern);
        } catch (Exception e) {
            log.error("Flink CDC 作业提交失败, mode={}, dsName={}", flinkConfig.getFlinkMode(), dsName, e);
        }
    }

    // ==================== 恢复与取消 ====================

    /**
     * 从数据库恢复已运行作业的映射（服务重启后调用）
     */
    private void recoverRunningJobs() {
        List<CdcFlinkJob> runningJobs = cdcFlinkJobRepository.findAllRunning();
        for (CdcFlinkJob job : runningJobs) {
            // 通过 cdcConfigId 反查 dsName
            CdcConfig config = cdcConfigRepository.findById(job.getCdcConfigId());
            if (config == null) {
                log.warn("恢复作业映射跳过: cdcConfigId={} 不存在, flinkJobId={}", job.getCdcConfigId(), job.getFlinkJobId());
                continue;
            }

            // remote 模式下校验作业是否仍在运行
            if ("remote".equalsIgnoreCase(flinkConfig.getFlinkMode())) {
                if (!checkRemoteJobRunning(job.getFlinkJobId())) {
                    job.setStatus(JobStatus.STOPPED);
                    job.setUpdatedAt(LocalDateTime.now());
                    job.update(cdcFlinkJobRepository);
                    log.info("远端 Flink 作业已停止，更新状态, flinkJobId={}", job.getFlinkJobId());
                    continue;
                }
            }

            dsNameToFlinkJobId.putIfAbsent(config.getDsName(), job.getFlinkJobId());
            log.info("恢复作业映射: dsName={}, flinkJobId={}", config.getDsName(), job.getFlinkJobId());
        }
        log.info("作业映射恢复完成, 共 {} 个运行中作业", dsNameToFlinkJobId.size());
    }

    /**
     * 检查远端 Flink 作业是否仍在运行
     */
    private boolean checkRemoteJobRunning(String flinkJobId) {
        try {
            String url = "http://" + flinkRestUrl + "/jobs/" + flinkJobId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("检查远端作业状态失败, flinkJobId={}, error={}", flinkJobId, e.getMessage());
            return false;
        }
    }

    /**
     * 远程模式：通过 REST API 取消作业
     */
    private void cancelRemoteFlinkJob(String flinkJobId) {
        try {
            String cancelUrl = "http://" + flinkRestUrl + "/jobs/" + flinkJobId + "/cancel";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cancelUrl))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 202) {
                log.info("Flink 作业取消请求发送成功，flinkJobId: {}", flinkJobId);
            } else {
                log.warn("取消 Flink 作业失败，flinkJobId: {}, status: {}, body: {}",
                        flinkJobId, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("取消 Flink 作业异常，flinkJobId: {}", flinkJobId, e);
        }
    }

    /**
     * 获取所有启用且使用 FLINK 同步的 CDC 配置
     */
    private List<CdcConfig> getEnabledFlinkConfigs() {
        CdcConfigListQuery query = new CdcConfigListQuery();
        query.setEnabled(true);
        query.setSyncTool(SyncTool.FLINK);
        return cdcConfigRepository.list(query);
    }
}
