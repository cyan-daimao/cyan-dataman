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
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * CDC Flink 同步服务实现（Application 模式）
 * <p>
 * 通过 Flink Kubernetes Operator 管理 FlinkDeployment CR，
 * 每个数据源对应一个独立的 Flink Application。
 * SQL 脚本通过 ConfigMap 挂载到 Pod，由 SqlRunner 执行。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class CdcFlinkSyncServiceImpl implements CdcFlinkSyncService {

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

    @Value("${flink.image:harbor.cyan.com/cyan/flink-sql:2.0.1}")
    private String flinkImage;

    @Value("${flink.namespace:flink}")
    private String flinkNamespace;

    private final CdcConfigRepository cdcConfigRepository;
    private final CdcFlinkJobRepository cdcFlinkJobRepository;
    private final KubernetesClient k8sClient;

    public CdcFlinkSyncServiceImpl(CdcConfigRepository cdcConfigRepository,
                                   CdcFlinkJobRepository cdcFlinkJobRepository) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcFlinkJobRepository = cdcFlinkJobRepository;
        this.k8sClient = new KubernetesClientBuilder().build();
    }

    @Override
    public void startFlinkSyncJob() {
        List<CdcConfig> enabledConfigs = getEnabledFlinkConfigs();
        Map<String, List<CdcConfig>> configsByDsName = new HashMap<>();
        for (CdcConfig config : enabledConfigs) {
            configsByDsName.computeIfAbsent(config.getDsName(), k -> new ArrayList<>()).add(config);
        }

        for (Map.Entry<String, List<CdcConfig>> entry : configsByDsName.entrySet()) {
            String dsName = entry.getKey();
            CdcConfig config = entry.getValue().getFirst();
            String subjectCode = config.getSubjectCode();
            String deploymentName = getDeploymentName(dsName, subjectCode);

            // 检查 FlinkDeployment 是否已存在
            if (getFlinkDeployment(deploymentName) != null) {
                log.info("数据源 {} 已有 FlinkDeployment，跳过提交", dsName);
                continue;
            }
            CompletableFuture.runAsync(() -> submitFlinkApplication(dsName, subjectCode, config));
        }
    }

    @Override
    public void stopFlinkSyncJob() {
        List<CdcFlinkJob> runningJobs = cdcFlinkJobRepository.findAllRunning();
        for (CdcFlinkJob job : runningJobs) {
            String deploymentName = getDeploymentName(job.getDsName(), job.getSubjectCode());
            deleteFlinkApplication(deploymentName);
            log.info("已删除 FlinkDeployment: {}", deploymentName);
        }
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
        String subjectCode = config.getSubjectCode();
        String deploymentName = getDeploymentName(dsName, subjectCode);

        if (getFlinkDeployment(deploymentName) == null) {
            CompletableFuture.runAsync(() -> submitFlinkApplication(dsName, subjectCode, config));
        } else {
            log.info("数据源 {} 已有 FlinkDeployment，新表数据将由 Kafka topic pattern 自动消费, table={}.{}",
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
        String subjectCode = config.getSubjectCode();
        log.info("已禁用 CDC 同步, dsName={}, table={}.{}",
                dsName, config.getDbName(), config.getTableName());

        // 检查该数据源下是否还有启用的表，没有则删除 FlinkDeployment
        List<CdcConfig> remainingEnabled = cdcConfigRepository.list(
                new CdcConfigListQuery().setDsName(dsName)
                        .setEnabled(true)
                        .setSyncTool(SyncTool.FLINK));

        if (remainingEnabled.isEmpty()) {
            String deploymentName = getDeploymentName(dsName, subjectCode);
            deleteFlinkApplication(deploymentName);
            log.info("数据源 {} 没有启用的表，已删除 FlinkDeployment", dsName);
        }
    }

    @Override
    public void cancelFlinkJob(String flinkJobId) {
        CdcFlinkJob flinkJob = cdcFlinkJobRepository.findByFlinkJobId(flinkJobId);
        if (flinkJob == null) {
            log.warn("Flink 作业不存在，flinkJobId: {}", flinkJobId);
            return;
        }

        String deploymentName = getDeploymentName(flinkJob.getDsName(), flinkJob.getSubjectCode());
        deleteFlinkApplication(deploymentName);

        flinkJob.setStatus(JobStatus.STOPPED);
        flinkJob.setUpdatedAt(LocalDateTime.now());
        flinkJob.update(cdcFlinkJobRepository);

        log.info("已取消 Flink CDC 作业，flinkJobId: {}", flinkJobId);
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    public void refreshSyncStatus() {
        List<CdcFlinkJob> runningJobs = cdcFlinkJobRepository.findAllRunning();
        for (CdcFlinkJob job : runningJobs) {
            try {
                String deploymentName = getDeploymentName(job.getDsName(), job.getSubjectCode());
                GenericKubernetesResource deployment = getFlinkDeployment(deploymentName);
                if (deployment == null) {
                    job.setStatus(JobStatus.STOPPED);
                    job.setUpdatedAt(LocalDateTime.now());
                    job.update(cdcFlinkJobRepository);
                    log.info("FlinkDeployment 已删除，更新状态: {}", deploymentName);
                }
            } catch (Exception e) {
                log.debug("检查 FlinkDeployment 状态失败: {}, error: {}", job.getFlinkJobId(), e.getMessage());
            }
        }
    }

    @Override
    public CdcFlinkJob findByDsName(String dsName) {
        return cdcFlinkJobRepository.findByDsName(dsName);
    }

    // ==================== Application 模式作业提交 ====================

    private void submitFlinkApplication(String dsName, String subjectCode, CdcConfig config) {
        String sql = buildFlinkSql(dsName, subjectCode);
        String deploymentName = getDeploymentName(dsName, subjectCode);
        String configMapName = getConfigMapName(dsName, subjectCode);

        try {
            // 1. 创建/更新 ConfigMap（包含 SQL 脚本）
            createOrUpdateConfigMap(configMapName, sql);
            log.info("ConfigMap 创建成功: {}", configMapName);

            // 2. 创建 FlinkDeployment CR
            String yaml = buildFlinkDeploymentYaml(deploymentName, configMapName, dsName, subjectCode);
            k8sClient.load(new java.io.ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
                    .inNamespace(flinkNamespace)
                    .createOrReplace();
            log.info("FlinkDeployment 创建成功: {}", deploymentName);

            // 3. 保存作业记录
            CdcFlinkJob flinkJob = new CdcFlinkJob()
                    .setDsName(dsName)
                    .setSubjectCode(subjectCode)
                    .setFlinkJobId(deploymentName)  // Application 模式用 deploymentName 作为标识
                    .setFlinkSql(sql)
                    .setEnabled(true)
                    .setStatus(JobStatus.RUNNING)
                    .setCreatedAt(LocalDateTime.now())
                    .setUpdatedAt(LocalDateTime.now())
                    .setCreateBy(config.getCreateBy())
                    .setUpdateBy(config.getUpdateBy());
            flinkJob.save(cdcFlinkJobRepository);

            log.info("Flink Application CDC 作业已提交, dsName={}, deploymentName={}", dsName, deploymentName);
        } catch (Exception e) {
            log.error("Flink Application CDC 作业提交失败, dsName={}", dsName, e);

            CdcFlinkJob failedJob = new CdcFlinkJob()
                    .setDsName(dsName)
                    .setSubjectCode(subjectCode)
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

    private void deleteFlinkApplication(String deploymentName) {
        try {
            k8sClient.genericKubernetesResources("flink.apache.org/v1beta1", "FlinkDeployment")
                    .inNamespace(flinkNamespace)
                    .withName(deploymentName)
                    .delete();

            // 同时删除关联的 ConfigMap
            String configMapName = deploymentName + "-sql";
            k8sClient.configMaps().inNamespace(flinkNamespace).withName(configMapName).delete();
        } catch (Exception e) {
            log.warn("删除 FlinkDeployment 失败: {}, error: {}", deploymentName, e.getMessage());
        }
    }

    private GenericKubernetesResource getFlinkDeployment(String name) {
        try {
            return k8sClient.genericKubernetesResources("flink.apache.org/v1beta1", "FlinkDeployment")
                    .inNamespace(flinkNamespace)
                    .withName(name)
                    .get();
        } catch (Exception e) {
            return null;
        }
    }

    private void createOrUpdateConfigMap(String name, String sql) {
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(flinkNamespace)
                .endMetadata()
                .addToData("job.sql", sql)
                .build();
        k8sClient.configMaps().inNamespace(flinkNamespace).createOrReplace(configMap);
    }

    // ==================== YAML 生成 ====================

    private String buildFlinkDeploymentYaml(String deploymentName, String configMapName,
                                            String dsName, String subjectCode) {
        String safeDsName = dsName.replaceAll("[^a-zA-Z0-9_]", "_");
        return String.format("""
                apiVersion: flink.apache.org/v1beta1
                kind: FlinkDeployment
                metadata:
                  name: %s
                  namespace: %s
                spec:
                  image: %s
                  flinkVersion: v2.0
                  jobManager:
                    resource:
                      memory: "2g"
                      cpu: 1
                  taskManager:
                    resource:
                      memory: "4g"
                      cpu: 2
                  flinkConfiguration:
                    state.backend: rocksdb
                    state.checkpoints.dir: s3://flink/checkpoints/cyan-dataman
                    state.savepoints.dir: s3://flink/savepoints/cyan-dataman
                    execution.checkpointing.interval: 60s
                    execution.checkpointing.timeout: 600s
                    execution.checkpointing.max-concurrent-checkpoints: 1
                    execution.checkpointing.min-pause: 500ms
                    execution.checkpointing.mode: EXACTLY_ONCE
                    s3.endpoint: %s
                    s3.access-key: %s
                    s3.secret-key: %s
                    s3.path.style.access: true
                  job:
                    jarURI: local:///opt/flink/usrlib/sql-runner.jar
                    entryClass: com.cyan.dataman.infra.flink.SqlRunner
                    args:
                      - "/opt/flink/sql/job.sql"
                    parallelism: 2
                    upgradeMode: stateful
                    state: running
                  podTemplate:
                    spec:
                      containers:
                        - name: flink-main-container
                          volumeMounts:
                            - name: sql-volume
                              mountPath: /opt/flink/sql
                      volumes:
                        - name: sql-volume
                          configMap:
                            name: %s
                """,
                deploymentName, flinkNamespace, flinkImage,
                rustfsEndpoint, rustfsAccessKey, rustfsSecretKey,
                configMapName);
    }

    private String getDeploymentName(String dsName, String subjectCode) {
        return "cdc-" + dsName.replaceAll("[^a-zA-Z0-9-]", "-") + "-" + subjectCode;
    }

    private String getConfigMapName(String dsName, String subjectCode) {
        return getDeploymentName(dsName, subjectCode) + "-sql";
    }

    // ==================== SQL 生成 ====================

    private String buildFlinkSql(String dsName, String subjectCode) {
        String safeDsName = dsName.replaceAll("[^a-zA-Z0-9_]", "_");
        String safeSubject = subjectCode.replaceAll("[^a-zA-Z0-9_]", "_");
        String odsTableName = "ods_cdc_raw_" + safeSubject + "_" + safeDsName;

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

                -- Iceberg ODS Sink：统一 Schema，纯追加，主题前缀=%s
                CREATE TABLE IF NOT EXISTS %s (
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
                INSERT INTO %s
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
                safeSubject, odsTableName, icebergRestUri, rustfsEndpoint, rustfsAccessKey, rustfsSecretKey,
                odsTableName, safeDsName);
    }

    // ==================== 工具方法 ====================

    private List<CdcConfig> getEnabledFlinkConfigs() {
        CdcConfigListQuery query = new CdcConfigListQuery();
        query.setEnabled(true);
        query.setSyncTool(SyncTool.FLINK);
        return cdcConfigRepository.list(query);
    }
}
