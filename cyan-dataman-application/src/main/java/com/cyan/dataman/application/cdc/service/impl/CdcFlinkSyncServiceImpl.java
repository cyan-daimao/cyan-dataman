package com.cyan.dataman.application.cdc.service.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.cdc.service.CdcFlinkSyncService;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcFlinkJob;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.cdc.repository.CdcFlinkJobRepository;
import com.cyan.dataman.enums.JobStatus;
import com.cyan.dataman.enums.SyncTool;
import com.cyan.dataman.infra.config.FlinkConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
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
 * CDC Flink 同步服务实现（Flink SQL 版本）
 * <p>
 * 架构：一数据源一 Flink SQL 作业，通过 Kafka topic pattern 消费该数据源下所有 topic，
 * 将完整的 Debezium JSON 写入统一的 ODS Iceberg 表。
 * <p>
 * ODS 表固定 Schema：
 * - _raw_json STRING    -- 完整 Debezium JSON
 * - _op STRING          -- 操作类型（c/u/d）
 * - _ts BIGINT          -- 数据变更时间戳
 * - _db STRING          -- 来源库名
 * - _table STRING       -- 来源表名
 * - _ingestion_time TIMESTAMP_LTZ(3)  -- 摄入时间
 * <p>
 * 支持 local 和 remote 模式：
 * - local: 在 Spring Boot 进程内本地运行 Flink 作业
 * - remote: 通过 RemoteStreamEnvironment 提交到远端 Flink 集群
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
            CdcConfig config = entry.getValue().getFirst();
            CompletableFuture.runAsync(() -> submitFlinkSqlJob(dsName, config));
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
        if (!dsNameToFlinkJobId.containsKey(dsName)) {
            CompletableFuture.runAsync(() -> submitFlinkSqlJob(dsName, config));
        } else {
            log.info("数据源 {} 已有运行中的 Flink 作业，新表数据将由 Kafka topic pattern 自动消费, table={}.{}",
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
        log.info("已禁用 CDC 同步, dsName={}, table={}.{}",
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

    // ==================== SQL 生成与作业提交 ====================

    /**
     * 生成 Flink SQL 并提交作业
     */
    private void submitFlinkSqlJob(String dsName, CdcConfig config) {
        String sql = buildFlinkSql(dsName);
        log.info("生成 Flink SQL，dsName={}\n{}", dsName, sql);

        StreamExecutionEnvironment env = flinkConfig.createStreamExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        try {
            // 执行 Kafka Source DDL
            String kafkaDdl = extractStatement(sql, "CREATE TABLE kafka_cdc_" + dsName);
            tableEnv.executeSql(kafkaDdl);
            log.info("Kafka Source 表创建成功，dsName={}", dsName);

            // 执行 Iceberg Sink DDL
            String icebergDdl = extractStatement(sql, "CREATE TABLE ods_cdc_raw_" + dsName);
            tableEnv.executeSql(icebergDdl);
            log.info("Iceberg Sink 表创建成功，dsName={}", dsName);

            // 执行 INSERT DML，获取 JobClient
            String insertDml = extractStatement(sql, "INSERT INTO ods_cdc_raw_" + dsName);
            TableResult result = tableEnv.executeSql(insertDml);

            String realJobId = result.getJobClient()
                    .map(client -> client.getJobID().toString())
                    .orElseThrow(() -> new RuntimeException("无法获取 Flink Job ID，可能作业提交失败"));

            // 保存作业记录
            CdcFlinkJob flinkJob = new CdcFlinkJob()
                    .setDsName(dsName)
                    .setFlinkJobId(realJobId)
                    .setFlinkSql(sql)
                    .setEnabled(true)
                    .setStatus(JobStatus.RUNNING)
                    .setCreatedAt(LocalDateTime.now())
                    .setUpdatedAt(LocalDateTime.now())
                    .setCreateBy(config.getCreateBy())
                    .setUpdateBy(config.getUpdateBy());
            flinkJob.save(cdcFlinkJobRepository);

            dsNameToFlinkJobId.put(dsName, realJobId);
            log.info("Flink SQL CDC 作业已提交, mode={}, dsName={}, flinkJobId={}",
                    flinkConfig.getFlinkMode(), dsName, realJobId);
        } catch (Exception e) {
            log.error("Flink SQL CDC 作业提交失败, mode={}, dsName={}", flinkConfig.getFlinkMode(), dsName, e);

            // 保存失败记录
            CdcFlinkJob failedJob = new CdcFlinkJob()
                    .setDsName(dsName)
                    .setFlinkSql(sql)
                    .setEnabled(false)
                    .setStatus(JobStatus.FAILED)
                    .setErrorMessage(e.getMessage())
                    .setCreatedAt(LocalDateTime.now())
                    .setUpdatedAt(LocalDateTime.now())
                    .setCreateBy(config.getCreateBy())
                    .setUpdateBy(config.getUpdateBy());
            failedJob.save(cdcFlinkJobRepository);
        }
    }

    /**
     * 构建 Flink SQL 文本
     */
    private String buildFlinkSql(String dsName) {
        String safeDsName = dsName.replaceAll("[^a-zA-Z0-9_]", "_");

        return String.format("""
                -- Kafka Source：使用 raw format 读取完整 Debezium JSON
                CREATE TABLE IF NOT EXISTS kafka_cdc_%s (
                  _raw_json STRING
                ) WITH (
                  'connector' = 'kafka',
                  'topic' = 'cdc-%s.*',
                  'properties.bootstrap.servers' = '%s',
                  'properties.group.id' = 'flink-cdc-ods-%s',
                  'scan.startup.mode' = 'earliest-offset',
                  'format' = 'raw'
                );

                -- Iceberg ODS Sink：统一 Schema，纯追加
                CREATE TABLE IF NOT EXISTS ods_cdc_raw_%s (
                  _raw_json STRING,
                  _op STRING,
                  _ts BIGINT,
                  _db STRING,
                  _table STRING,
                  _ingestion_time TIMESTAMP_LTZ(3)
                ) WITH (
                  'connector' = 'iceberg',
                  'catalog-name' = 'rest',
                  'catalog-type' = 'rest',
                  'uri' = '%s',
                  'warehouse' = 's3://lakehouse/ods',
                  'format-version' = '2',
                  'write.format.default' = 'parquet',
                  'write.upsert.enabled' = 'false',
                  's3.endpoint' = '%s',
                  's3.access-key-id' = '%s',
                  's3.secret-access-key' = '%s',
                  's3.path-style-access' = 'true'
                );

                -- 将 Debezium JSON 写入 ODS，同时提取元数据字段
                INSERT INTO ods_cdc_raw_%s
                SELECT
                  _raw_json,
                  COALESCE(JSON_VALUE(_raw_json, '$.payload.op'), JSON_VALUE(_raw_json, '$.op')) AS _op,
                  CAST(COALESCE(JSON_VALUE(_raw_json, '$.payload.ts_ms'), JSON_VALUE(_raw_json, '$.ts_ms')) AS BIGINT) AS _ts,
                  COALESCE(JSON_VALUE(_raw_json, '$.payload.source.db'), JSON_VALUE(_raw_json, '$.source.db')) AS _db,
                  COALESCE(JSON_VALUE(_raw_json, '$.payload.source.table'), JSON_VALUE(_raw_json, '$.source.table')) AS _table,
                  NOW() AS _ingestion_time
                FROM kafka_cdc_%s;
                """,
                safeDsName, dsName, kafkaBootstrapServers, dsName,
                safeDsName, icebergRestUri, rustfsEndpoint, rustfsAccessKey, rustfsSecretKey,
                safeDsName, safeDsName);
    }

    /**
     * 从完整 SQL 文本中提取单条语句
     */
    private String extractStatement(String fullSql, String statementPrefix) {
        String[] statements = fullSql.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.toUpperCase().startsWith(statementPrefix.toUpperCase()) ||
                trimmed.toUpperCase().contains(statementPrefix.toUpperCase())) {
                return trimmed;
            }
        }
        throw new RuntimeException("SQL 中未找到以 '" + statementPrefix + "' 开头的语句");
    }

    // ==================== 恢复与取消 ====================

    /**
     * 从数据库恢复已运行作业的映射（服务重启后调用）
     */
    private void recoverRunningJobs() {
        List<CdcFlinkJob> runningJobs = cdcFlinkJobRepository.findAllRunning();
        for (CdcFlinkJob job : runningJobs) {
            String dsName = job.getDsName();
            if (dsName == null) {
                log.warn("恢复作业映射跳过: flinkJobId={} 的 dsName 为空", job.getFlinkJobId());
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

            dsNameToFlinkJobId.putIfAbsent(dsName, job.getFlinkJobId());
            log.info("恢复作业映射: dsName={}, flinkJobId={}", dsName, job.getFlinkJobId());
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

    @Override
    public CdcFlinkJob findByDsName(String dsName) {
        return cdcFlinkJobRepository.findByDsName(dsName);
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
