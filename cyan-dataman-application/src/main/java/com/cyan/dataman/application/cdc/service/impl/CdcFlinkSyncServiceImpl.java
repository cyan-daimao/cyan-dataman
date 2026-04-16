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
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
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
 * 实现单个表级别的 Flink CDC 同步开启/关闭。
 * 支持本地模式和远程模式：
 * - 本地模式（flink.mode=local）：直接在本地运行 Flink 作业，用于调试
 * - 远程模式（flink.mode=remote）：通过 Flink REST API 管理远程集群，Spring Boot 重启不影响远程集群
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class CdcFlinkSyncServiceImpl implements CdcFlinkSyncService {

    /**
     * Flink 运行环境模式：local=本地模式，remote=远程模式
     */
    @Value("${flink.mode:local}")
    private String flinkMode;

    /**
     * Flink REST API 地址（远程模式使用）
     */
    @Value("${flink.rest.url:http://10.0.0.2:20031}")
    private String flinkRestUrl;

    /**
     * Kafka 地址
     */
    @Value("${kafka.url:kafka:9092}")
    private String kafkaBootstrapServers;

    /**
     * Iceberg REST Catalog URI
     */
    @Value("${iceberg.uri:http://iceberg-rest.cyan.com/iceberg}")
    private String icebergRestUri;

    /**
     * RustFS (S3) endpoint
     */
    @Value("${rustfs.endpoint:http://10.0.0.2:9000}")
    private String rustfsEndpoint;

    /**
     * RustFS access key
     */
    @Value("${rustfs.accessKey:rustfsadmin}")
    private String rustfsAccessKey;

    /**
     * RustFS secret key
     */
    @Value("${rustfs.secretKey:rustfsadmin}")
    private String rustfsSecretKey;

    /**
     * 存储数据源名称到其运行中的 Flink 作业 ID 的映射
     */
    private final Map<String, String> dsNameToFlinkJobId = new ConcurrentHashMap<>();

    /**
     * 存储数据源名称到其对应的 CDC 配置 ID 列表的映射
     */
    private final Map<String, Set<String>> dsNameToCdcConfigIds = new ConcurrentHashMap<>();

    private final CdcConfigRepository cdcConfigRepository;
    private final CdcFlinkJobRepository cdcFlinkJobRepository;
    private final MetadataTableRepository metadataTableRepository;
    private final StreamExecutionEnvironment streamExecutionEnvironment;
    private final HttpClient httpClient;

    public CdcFlinkSyncServiceImpl(FlinkConfig flinkConfig,
                                   CdcConfigRepository cdcConfigRepository,
                                   CdcFlinkJobRepository cdcFlinkJobRepository,
                                   MetadataTableRepository metadataTableRepository) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcFlinkJobRepository = cdcFlinkJobRepository;
        this.metadataTableRepository = metadataTableRepository;
        this.streamExecutionEnvironment = flinkConfig.streamExecutionEnvironment();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }


    @Override
    public void startFlinkSyncJob() {
        List<CdcConfig> enabledFlinkConfigs = getEnabledFlinkConfigs();
        if (enabledFlinkConfigs.isEmpty()) {
            log.info("没有启用的 FLINK CDC 配置，无需启动作业");
            return;
        }

        Map<String, List<CdcConfig>> configsByDsName = new HashMap<>();
        for (CdcConfig config : enabledFlinkConfigs) {
            configsByDsName.computeIfAbsent(config.getDsName(), k -> new ArrayList<>()).add(config);
        }

        for (Map.Entry<String, List<CdcConfig>> entry : configsByDsName.entrySet()) {
            String dsName = entry.getKey();
            List<CdcConfig> configs = entry.getValue();
            for (CdcConfig config : configs) {
                enableCdcSync(config.getId());
            }
        }
    }

    @Override
    public void stopFlinkSyncJob() {
        List<CdcFlinkJob> runningJobs = cdcFlinkJobRepository.findAllRunning();

        for (CdcFlinkJob job : runningJobs) {
            cancelFlinkJob(job.getFlinkJobId());
        }

        dsNameToFlinkJobId.clear();
        dsNameToCdcConfigIds.clear();

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
        String existingFlinkJobId = dsNameToFlinkJobId.get(dsName);

        if (existingFlinkJobId != null) {
            log.info("数据源 {} 的 Flink 作业已存在，dsName: {}, flinkJobId: {}",dsName, dsName, existingFlinkJobId);
        } else {
            if ("local".equalsIgnoreCase(flinkMode)) {
                // 异步启动 Flink 作业，避免阻塞 HTTP 请求线程
                CdcConfig finalConfig = config;
                CompletableFuture.runAsync(() -> createLocalFlinkJob(dsName, finalConfig));
            } else {
                createRemoteFlinkJob(dsName, config);
            }
        }

        dsNameToCdcConfigIds.computeIfAbsent(dsName, k -> ConcurrentHashMap.newKeySet()).add(cdcConfigId);
    }

    @Override
    public void disableCdcSync(String cdcConfigId) {
        CdcConfig config = cdcConfigRepository.findById(cdcConfigId);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        if (Boolean.TRUE.equals(config.getEnabled())) {
            config.toggle(cdcConfigRepository, false);
        }

        String dsName = config.getDsName();
        Set<String> configIds = dsNameToCdcConfigIds.get(dsName);
        if (configIds != null) {
            configIds.remove(cdcConfigId);
        }

        List<CdcConfig> remainingEnabledConfigs = cdcConfigRepository.list(
                new CdcConfigListQuery().setDsName(dsName)
                        .setEnabled(true)
                        .setSyncTool(SyncTool.FLINK));

        if (remainingEnabledConfigs.isEmpty()) {
            String flinkJobId = dsNameToFlinkJobId.get(dsName);
            if (flinkJobId != null) {
                cancelFlinkJob(flinkJobId);
            }
            log.info("数据源 {} 没有其他启用的 CDC 配置，停止 Flink 作业", dsName);
        } else {
            log.info("动态停止表的同步（本地模式暂不支持），dsName: {}, table: {}.{}",
                    dsName, config.getDbName(), config.getTableName());
        }
    }

    @Override
    public void cancelFlinkJob(String flinkJobId) {
        CdcFlinkJob flinkJob = cdcFlinkJobRepository.findByFlinkJobId(flinkJobId);
        if (flinkJob == null) {
            log.warn("Flink 作业不存在，flinkJobId: {}", flinkJobId);
            return;
        }

        if ("remote".equalsIgnoreCase(flinkMode)) {
            cancelRemoteFlinkJob(flinkJobId);
        } else {
            log.info("本地模式下停止作业（作业随 Spring Boot 停止而停止），flinkJobId: {}", flinkJobId);
        }

        flinkJob.setStatus(JobStatus.STOPPED);
        flinkJob.setUpdatedAt(LocalDateTime.now());
        flinkJob.update(cdcFlinkJobRepository);

        dsNameToFlinkJobId.entrySet().removeIf(entry -> entry.getValue().equals(flinkJobId));
        dsNameToCdcConfigIds.clear();

        log.info("已取消 Flink CDC 作业，flinkJobId: {}", flinkJobId);
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    public void refreshSyncStatus() {
        if ("remote".equalsIgnoreCase(flinkMode)) {
            List<CdcFlinkJob> runningJobs = cdcFlinkJobRepository.findAllRunning();

            for (CdcFlinkJob job : runningJobs) {
                try {
                    String jobUrl = flinkRestUrl + "/jobs/" + job.getFlinkJobId();
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
                        log.info("Flink 作业已结束，更新状态，flinkJobId: {}", job.getFlinkJobId());
                    }
                } catch (Exception e) {
                    log.debug("检查 Flink 作业状态失败，flinkJobId: {}, error: {}", job.getFlinkJobId(), e.getMessage());
                }
            }
        }
    }

    /**
     * 本地模式：创建并运行 Flink 作业
     */
    private void createLocalFlinkJob(String dsName, CdcConfig config) {
        String flinkJobId = UUID.randomUUID().toString();

        CdcFlinkJob flinkJob = new CdcFlinkJob()
                .setFlinkJobId(flinkJobId)
                .setCdcConfigId(config.getId())
                .setEnabled(true)
                .setStatus(JobStatus.RUNNING)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now())
                .setCreateBy(config.getCreateBy())
                .setUpdateBy(config.getUpdateBy());
        flinkJob = flinkJob.save(cdcFlinkJobRepository);

        dsNameToFlinkJobId.put(dsName, flinkJobId);

        log.info("本地模式创建 Flink 作业，dsName: {}, flinkJobId: {}", dsName, flinkJobId);

        // 获取该数据源下所有启用的 CDC 配置
        List<CdcConfig> allConfigs = cdcConfigRepository.list(
                new CdcConfigListQuery().setDsName(dsName)
                        .setEnabled(true)
                        .setSyncTool(SyncTool.FLINK));

        // 构建 Kafka topic 列表
        // Debezium 生成的 topic 格式为: {connectorName}.{dbName}.{tableName}
        // 例如: cdc-dsName.cyan_dataman.metadata_table
        List<String> topics = allConfigs.stream()
                .map(c -> c.getConnectorName() + "." + c.getDbName() + "." + c.getTableName())
                .toList();

        // 等待 Debezium 完成 topic 创建（增量快照信号触发后，Debezium 需要时间执行快照并写入 Kafka）
        waitForTopicsCreated(topics, 120);

        // 构建启用表的键集合（用于序列化传递，格式: dbName.tableName）
        // 注意：这里不要加 connectorName 前缀，因为 CdcProcessFunction 是从 JSON payload 中提取表信息
        Set<String> enabledTableKeys = allConfigs.stream()
                .map(c -> c.getDbName() + "." + c.getTableName())
                .collect(java.util.stream.Collectors.toSet());

        // 构建完整表映射：dbName.tableName -> icebergSchema.icebergTableName
        // 需要从 MetadataTable 查询 Iceberg 表的 schema 信息
        Map<String, String> tableToIcebergMapping = new HashMap<>();
        for (CdcConfig cdcConfig : allConfigs) {
            String mysqlTableKey = cdcConfig.getDbName() + "." + cdcConfig.getTableName();
            String icebergTableName = cdcConfig.getIcebergTableName();

            // 解析 icebergTableName（可能是 "schema.tableName" 或纯表名）
            String tableName;
            if (icebergTableName.contains(".")) {
                String[] parts = icebergTableName.split("\\.");
                tableName = parts[parts.length - 1];
            } else {
                tableName = icebergTableName;
            }

            // 查询元数据表获取 Iceberg schema
            MetadataTable metadataTable = metadataTableRepository.findOne(
                    new MetadataTableOneQuery().setName(tableName));

            if (metadataTable == null || metadataTable.getTable() == null) {
                log.warn("元数据表不存在，跳过: {}", tableName);
                continue;
            }

            String icebergSchema = metadataTable.getTable().getSchema();
            String icebergTableKey = icebergSchema + "." + tableName;
            tableToIcebergMapping.put(mysqlTableKey, icebergTableKey);

            log.info("表映射: {} -> {}", mysqlTableKey, icebergTableKey);
        }

        // 创建 Kafka Source
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setBootstrapServers(kafkaBootstrapServers)
                .setTopics(topics)
                .setGroupId("flink-cdc-consumer-" + dsName)
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .setStartingOffsets(OffsetsInitializer.earliest())
                .build();

        DataStream<String> rawStream = streamExecutionEnvironment.fromSource(
                        kafkaSource,
                        WatermarkStrategy.noWatermarks(),
                        "Kafka CDC Source - " + dsName)
                .uid("kafka-source-" + dsName);

        // 使用新的批量写入 Sink
        // 1. 解析 Debezium 消息并转换为 IcebergWriteRecord
        DataStream<IcebergWriteRecord> recordStream = rawStream
                .process(new DebeziumToIcebergProcessFunction(
                        dsName,
                        tableToIcebergMapping,
                        icebergRestUri,
                        rustfsEndpoint,
                        rustfsAccessKey,
                        rustfsSecretKey))
                .uid("cdc-parse-" + dsName)
                .name("CDC Parse - " + dsName);

        // 2. 使用批量写入（利用 Checkpoint 批量提交）
        // 目标文件大小：128MB
        recordStream
                .process(new IcebergBatchSink(
                        icebergRestUri,
                        rustfsEndpoint,
                        rustfsAccessKey,
                        rustfsSecretKey,
                        128 * 1024 * 1024L
                ))
                .name("Iceberg Sink - " + dsName)
                .uid("iceberg-sink-" + dsName);

        try {
            streamExecutionEnvironment.execute("Flink CDC Sync - " + dsName);
            log.info("本地 Flink CDC 作业已启动，dsName: {}, flinkJobId: {}", dsName, flinkJobId);
        } catch (Exception e) {
            log.error("本地 Flink CDC 作业启动失败，dsName: {}", dsName, e);
            flinkJob.setStatus(JobStatus.FAILED);
            flinkJob.setErrorMessage(e.getMessage());
            flinkJob.update(cdcFlinkJobRepository);
        }
    }

    /**
     * 远程模式：通过 REST API 创建 Flink 作业
     */
    private void createRemoteFlinkJob(String dsName, CdcConfig config) {
        String flinkJobId = UUID.randomUUID().toString();

        CdcFlinkJob flinkJob = new CdcFlinkJob();
        // id 由 MyBatis Plus 雪花算法自动生成
        flinkJob.setFlinkJobId(flinkJobId);
        flinkJob.setCdcConfigId(config.getId());
        flinkJob.setEnabled(true);
        flinkJob.setStatus(JobStatus.RUNNING);
        flinkJob.setCreatedAt(LocalDateTime.now());
        flinkJob.setUpdatedAt(LocalDateTime.now());
        flinkJob.save(cdcFlinkJobRepository);

        dsNameToFlinkJobId.put(dsName, flinkJobId);

        log.info("远程模式创建 Flink 作业（需要通过 Flink 客户端提交），dsName: {}, flinkJobId: {}", dsName, flinkJobId);
    }

    /**
     * 远程模式：通过 REST API 取消作业
     */
    private void cancelRemoteFlinkJob(String flinkJobId) {
        try {
            String cancelUrl = flinkRestUrl + "/jobs/" + flinkJobId + "/cancel";
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

    /**
     * 等待 Kafka topic 被创建
     * Debezium connector 创建后异步执行快照，topic 不会立即可用，需要轮询等待
     *
     * @param topics          期望存在的 topic 列表
     * @param timeoutSeconds  最大等待秒数
     */
    private void waitForTopicsCreated(List<String> topics, int timeoutSeconds) {
        if (topics.isEmpty()) {
            return;
        }
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaBootstrapServers);
        props.put("request.timeout.ms", "5000");
        props.put("default.api.timeout.ms", "5000");

        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        try (var admin = org.apache.kafka.clients.admin.AdminClient.create(props)) {
            while (System.currentTimeMillis() < deadline) {
                try {
                    var existing = admin.listTopics().names().get(5, java.util.concurrent.TimeUnit.SECONDS);
                    if (existing.containsAll(topics)) {
                        log.info("Kafka topic 已就绪: {}", topics);
                        return;
                    }
                    log.info("等待 Kafka topic 创建，已有: {}, 期望: {}", existing, topics);
                } catch (Exception e) {
                    log.warn("查询 Kafka topic 失败: {}", e.getMessage());
                }
                Thread.sleep(2000);
            }
            log.warn("等待 Kafka topic 超时 ({}s)，继续启动 Flink 作业，期望: {}", timeoutSeconds, topics);
        } catch (Exception e) {
            log.warn("Kafka AdminClient 异常: {}", e.getMessage());
        }
    }
}
