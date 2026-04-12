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
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
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
     * 存储数据源名称到其运行中的 Flink 作业 ID 的映射
     */
    private final Map<String, String> dsNameToFlinkJobId = new ConcurrentHashMap<>();

    /**
     * 存储数据源名称到其对应的 CDC 配置 ID 列表的映射
     */
    private final Map<String, Set<String>> dsNameToCdcConfigIds = new ConcurrentHashMap<>();

    private final CdcConfigRepository cdcConfigRepository;
    private final CdcFlinkJobRepository cdcFlinkJobRepository;
    private final StreamExecutionEnvironment streamExecutionEnvironment;
    private final HttpClient httpClient;

    public CdcFlinkSyncServiceImpl(FlinkConfig flinkConfig,
                                   CdcConfigRepository cdcConfigRepository,
                                   CdcFlinkJobRepository cdcFlinkJobRepository) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcFlinkJobRepository = cdcFlinkJobRepository;
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
                createLocalFlinkJob(dsName, config);
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

        // 构建启用表的键集合（用于序列化传递，格式: dbName.tableName）
        // 注意：这里不要加 connectorName 前缀，因为 CdcProcessFunction 是从 JSON payload 中提取表信息
        Set<String> enabledTableKeys = allConfigs.stream()
                .map(c -> c.getDbName() + "." + c.getTableName())
                .collect(java.util.stream.Collectors.toSet());

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

        SingleOutputStreamOperator<String> processedStream = rawStream
                .process(new CdcProcessFunction(enabledTableKeys))
                .uid("cdc-process-" + dsName)
                .name("CDC Process - " + dsName);

        // 直接使用 print()，Flink 2.0 会自动处理 Sink 算子
        processedStream.print();

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
     * CDC 处理函数
     * 必须为静态类，且只包含可序列化字段
     */
    private static class CdcProcessFunction extends ProcessFunction<String, String> {

        /**
         * 启用表的键集合（格式：dbName.tableName）
         */
        private final Set<String> enabledTables;

        public CdcProcessFunction(Set<String> enabledTables) {
            this.enabledTables = enabledTables;
        }

        @Override
        public void processElement(String value, Context ctx, Collector<String> out) {
            String tableKey = extractTableKey(value);
            if (tableKey == null) {
                return;
            }

            if (!enabledTables.contains(tableKey)) {
                return;
            }

            out.collect(value);
        }

        private String extractTableKey(String json) {
            try {
                int dbIdx = json.indexOf("\"db\":\"");
                int tableIdx = json.indexOf("\"table\":\"");
                if (dbIdx == -1 || tableIdx == -1) {
                    return null;
                }
                int dbStart = dbIdx + 5;
                int dbEnd = json.indexOf("\"", dbStart);
                int tableStart = tableIdx + 8;
                int tableEnd = json.indexOf("\"", tableStart);
                String db = json.substring(dbStart, dbEnd);
                String table = json.substring(tableStart, tableEnd);
                return db + "." + table;
            } catch (Exception e) {
                return null;
            }
        }
    }
}
