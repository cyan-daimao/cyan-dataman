package com.cyan.dataman.application.cdc.service.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.cdc.service.CdcFlinkSyncService;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcFlinkJob;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.cdc.repository.CdcFlinkJobRepository;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.TableValObj;
import com.cyan.dataman.enums.*;
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
 * 通过 Flink Kubernetes Operator 管理 FlinkDeployment CR。
 * 一数据源 + 一主题对应一个 FlinkDeployment，作业内通过 StatementSet
 * 共享 Kafka Source，每个 CDC 表有独立的 Iceberg ODS Sink。
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
    private final MetadataTableService metadataTableService;
    private final KubernetesClient k8sClient;

    public CdcFlinkSyncServiceImpl(CdcConfigRepository cdcConfigRepository,
                                   CdcFlinkJobRepository cdcFlinkJobRepository,
                                   MetadataTableService metadataTableService) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcFlinkJobRepository = cdcFlinkJobRepository;
        this.metadataTableService = metadataTableService;
        this.k8sClient = new KubernetesClientBuilder().build();
    }

    @Override
    public void startFlinkSyncJob() {
        List<CdcConfig> enabledConfigs = getEnabledFlinkConfigs();
        Map<String, List<CdcConfig>> configsByGroup = new HashMap<>();
        for (CdcConfig config : enabledConfigs) {
            String key = config.getDsName() + ":" + config.getSubjectCode();
            configsByGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(config);
        }

        for (Map.Entry<String, List<CdcConfig>> entry : configsByGroup.entrySet()) {
            List<CdcConfig> configs = entry.getValue();
            CdcConfig first = configs.getFirst();
            String dsName = first.getDsName();
            String subjectCode = first.getSubjectCode();
            String deploymentName = getDeploymentName(dsName, subjectCode);

            if (getFlinkDeployment(deploymentName) != null) {
                log.info("数据源 {} 主题 {} 已有 FlinkDeployment，跳过提交", dsName, subjectCode);
                continue;
            }
            CompletableFuture.runAsync(() -> submitFlinkApplication(dsName, subjectCode, configs));
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
        String configMapName = getConfigMapName(dsName, subjectCode);

        if (getFlinkDeployment(deploymentName) == null) {
            // 没有 Deployment，创建新的（包含该组下所有启用的表）
            List<CdcConfig> configs = getEnabledConfigsByDsAndSubject(dsName, subjectCode);
            CompletableFuture.runAsync(() -> submitFlinkApplication(dsName, subjectCode, configs));
        } else {
            // 已有 Deployment，动态添加 Sink
            CdcFlinkJob job = cdcFlinkJobRepository.findByDsNameAndSubjectCode(dsName, subjectCode);
            if (job != null) {
                String currentSql = job.getFlinkSql();
                String sinkMarker = sinkMarker(config.getDbName(), config.getTableName());
                if (!currentSql.contains(sinkMarker)) {
                    String safeDsName = safeName(dsName);
                    String newSinkSql = buildSinkSql(config, safeDsName);
                    String updatedSql = currentSql + "\n" + newSinkSql;
                    createOrUpdateConfigMap(configMapName, updatedSql);
                    job.setFlinkSql(updatedSql);
                    job.setUpdatedAt(LocalDateTime.now());
                    job.update(cdcFlinkJobRepository);
                    log.info("已向 FlinkDeployment {} 添加 Sink: {}.{}", deploymentName, config.getDbName(), config.getTableName());
                }
            }
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
        String deploymentName = getDeploymentName(dsName, subjectCode);
        String configMapName = getConfigMapName(dsName, subjectCode);

        log.info("已禁用 CDC 同步, dsName={}, table={}.{}", dsName, config.getDbName(), config.getTableName());

        // 检查该数据源+主题下是否还有启用的表
        List<CdcConfig> remainingEnabled = getEnabledConfigsByDsAndSubject(dsName, subjectCode);

        if (remainingEnabled.isEmpty()) {
            deleteFlinkApplication(deploymentName);
            CdcFlinkJob job = cdcFlinkJobRepository.findByDsNameAndSubjectCode(dsName, subjectCode);
            if (job != null) {
                job.stop(cdcFlinkJobRepository);
            }
            log.info("数据源 {} 主题 {} 没有启用的表，已删除 FlinkDeployment", dsName, subjectCode);
        } else {
            CdcFlinkJob job = cdcFlinkJobRepository.findByDsNameAndSubjectCode(dsName, subjectCode);
            if (job != null) {
                String updatedSql = removeSinkFromSql(job.getFlinkSql(), config.getDbName(), config.getTableName());
                createOrUpdateConfigMap(configMapName, updatedSql);
                job.setFlinkSql(updatedSql);
                job.setUpdatedAt(LocalDateTime.now());
                job.update(cdcFlinkJobRepository);
                log.info("已从 FlinkDeployment {} 移除 Sink: {}.{}", deploymentName, config.getDbName(), config.getTableName());
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
        // 保持接口兼容，返回该数据源下第一个主题的 FlinkJob
        List<CdcConfig> configs = getEnabledConfigsByDsAndSubject(dsName, null);
        if (configs.isEmpty()) {
            return null;
        }
        String subjectCode = configs.getFirst().getSubjectCode();
        return cdcFlinkJobRepository.findByDsNameAndSubjectCode(dsName, subjectCode);
    }

    // ==================== Application 模式作业提交 ====================

    private void submitFlinkApplication(String dsName, String subjectCode, List<CdcConfig> configs) {
        String sql = buildFlinkSql(dsName, subjectCode, configs);
        String deploymentName = getDeploymentName(dsName, subjectCode);
        String configMapName = getConfigMapName(dsName, subjectCode);
        CdcConfig first = configs.getFirst();

        try {
            // 先通过元数据平台创建 ODS 表（Flink 只负责写入，不负责建表）
            for (CdcConfig config : configs) {
                ensureOdsTableExists(config);
            }

            createOrUpdateConfigMap(configMapName, sql);
            log.info("ConfigMap 创建/更新成功: {}", configMapName);

            String yaml = buildFlinkDeploymentYaml(deploymentName, configMapName);
            k8sClient.load(new java.io.ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
                    .inNamespace(flinkNamespace)
                    .createOrReplace();
            log.info("FlinkDeployment 创建/更新成功: {}", deploymentName);

            CdcFlinkJob flinkJob = cdcFlinkJobRepository.findByDsNameAndSubjectCode(dsName, subjectCode);
            if (flinkJob == null) {
                flinkJob = new CdcFlinkJob()
                        .setDsName(dsName)
                        .setSubjectCode(subjectCode)
                        .setFlinkJobId(deploymentName)
                        .setFlinkSql(sql)
                        .setEnabled(true)
                        .setStatus(JobStatus.RUNNING)
                        .setCreatedAt(LocalDateTime.now())
                        .setUpdatedAt(LocalDateTime.now())
                        .setCreateBy(first.getCreateBy())
                        .setUpdateBy(first.getUpdateBy());
                flinkJob.save(cdcFlinkJobRepository);
            } else {
                flinkJob.setFlinkSql(sql)
                        .setEnabled(true)
                        .setStatus(JobStatus.RUNNING)
                        .setErrorMessage("")
                        .setUpdatedAt(LocalDateTime.now())
                        .setUpdateBy(first.getUpdateBy());
                flinkJob.update(cdcFlinkJobRepository);
            }

            log.info("Flink CDC 作业已提交, dsName={}, subjectCode={}, tables={}",
                    dsName, subjectCode, configs.size());
        } catch (Exception e) {
            log.error("Flink CDC 作业提交失败, dsName={}, subjectCode={}", dsName, subjectCode, e);

            CdcFlinkJob failedJob = cdcFlinkJobRepository.findByDsNameAndSubjectCode(dsName, subjectCode);
            if (failedJob == null) {
                failedJob = new CdcFlinkJob()
                        .setDsName(dsName)
                        .setSubjectCode(subjectCode)
                        .setFlinkSql(sql)
                        .setEnabled(false)
                        .setStatus(JobStatus.FAILED)
                        .setErrorMessage(e.getMessage())
                        .setCreatedAt(LocalDateTime.now())
                        .setUpdatedAt(LocalDateTime.now())
                        .setCreateBy(first.getCreateBy())
                        .setUpdateBy(first.getUpdateBy());
                failedJob.save(cdcFlinkJobRepository);
            } else {
                failedJob.setStatus(JobStatus.FAILED)
                        .setErrorMessage(e.getMessage())
                        .setUpdatedAt(LocalDateTime.now());
                failedJob.update(cdcFlinkJobRepository);
            }
        }
    }

    private void deleteFlinkApplication(String deploymentName) {
        try {
            k8sClient.genericKubernetesResources("flink.apache.org/v1beta1", "FlinkDeployment")
                    .inNamespace(flinkNamespace)
                    .withName(deploymentName)
                    .delete();

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

    private String buildFlinkDeploymentYaml(String deploymentName, String configMapName) {
        return String.format("""
                apiVersion: flink.apache.org/v1beta1
                kind: FlinkDeployment
                metadata:
                  name: %s
                  namespace: %s
                spec:
                  serviceAccount: flink
                  image: %s
                  flinkVersion: v2_0
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
                    upgradeMode: last-state
                    state: running
                  podTemplate:
                    spec:
                      imagePullSecrets:
                        - name: harbor-secret
                      containers:
                        - name: flink-main-container
                          imagePullPolicy: Always
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

    private String buildFlinkSql(String dsName, String subjectCode, List<CdcConfig> configs) {
        String safeDsName = safeName(dsName);

        StringBuilder sql = new StringBuilder();
        sql.append(String.format("""
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
                """, safeDsName, dsName, kafkaBootstrapServers, dsName));

        for (CdcConfig config : configs) {
            sql.append("\n").append(buildSinkSql(config, safeDsName));
        }

        return sql.toString();
    }

    private String buildSinkSql(CdcConfig config, String safeDsName) {
        String safeSubject = safeName(config.getSubjectCode());
        String safeDb = safeName(config.getDbName());
        String safeTable = safeName(config.getTableName());
        String odsTableName = "ods_cdc_raw_" + safeSubject + "_" + safeDb + "_" + safeTable;

        return String.format("""
                -- ==== Sink: %s.%s ====
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
                  'warehouse' = 's3://warehouse/ods',
                  'format-version' = '2',
                  'write.format.default' = 'parquet',
                  'write.upsert.enabled' = 'false',
                  's3.endpoint' = '%s',
                  's3.access-key-id' = '%s',
                  's3.secret-access-key' = '%s',
                  's3.path-style-access' = 'true'
                );

                INSERT INTO %s
                SELECT
                  _raw_json,
                  COALESCE(JSON_VALUE(_raw_json, '$.payload.op'), JSON_VALUE(_raw_json, '$.op')) AS _op,
                  CAST(COALESCE(JSON_VALUE(_raw_json, '$.payload.ts_ms'), JSON_VALUE(_raw_json, '$.ts_ms')) AS BIGINT) AS _ts,
                  COALESCE(JSON_VALUE(_raw_json, '$.payload.source.db'), JSON_VALUE(_raw_json, '$.source.db')) AS _db,
                  COALESCE(JSON_VALUE(_raw_json, '$.payload.source.table'), JSON_VALUE(_raw_json, '$.source.table')) AS _table,
                  NOW() AS _ingestion_time
                FROM kafka_cdc_%s
                WHERE COALESCE(JSON_VALUE(_raw_json, '$.payload.source.table'), JSON_VALUE(_raw_json, '$.source.table')) = '%s'
                  AND COALESCE(JSON_VALUE(_raw_json, '$.payload.source.db'), JSON_VALUE(_raw_json, '$.source.db')) = '%s';
                -- ==== End Sink: %s.%s ====
                """,
                config.getDbName(), config.getTableName(),
                odsTableName, icebergRestUri, rustfsEndpoint, rustfsAccessKey, rustfsSecretKey,
                odsTableName, safeDsName, config.getTableName(), config.getDbName(),
                config.getDbName(), config.getTableName());
    }

    private String removeSinkFromSql(String sql, String dbName, String tableName) {
        String startMarker = "-- ==== Sink: " + dbName + "." + tableName + " ====";
        String endMarker = "-- ==== End Sink: " + dbName + "." + tableName + " ====";

        int startIndex = sql.indexOf(startMarker);
        if (startIndex == -1) {
            return sql;
        }
        int endIndex = sql.indexOf(endMarker, startIndex);
        if (endIndex == -1) {
            return sql;
        }
        return sql.substring(0, startIndex).trim() + "\n" + sql.substring(endIndex + endMarker.length()).trim();
    }

    private String sinkMarker(String dbName, String tableName) {
        return "-- ==== Sink: " + dbName + "." + tableName + " ====";
    }

    private String safeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * 确保 ODS 表已存在（通过元数据平台创建，Flink 只负责写入）
     */
    private void ensureOdsTableExists(CdcConfig config) {
        String safeSubject = safeName(config.getSubjectCode());
        String safeDb = safeName(config.getDbName());
        String safeTable = safeName(config.getTableName());
        String odsTableName = "ods_cdc_raw_" + safeSubject + "_" + safeDb + "_" + safeTable;

        try {
            MetadataTableCmd cmd = new MetadataTableCmd()
                    .setName(odsTableName)
                    .setOwner(config.getCreateBy())
                    .setSubjectCode(config.getSubjectCode())
                    .setLayerCode(DataLayer.ODS)
                    .setComment("CDC ODS 表: " + config.getDbName() + "." + config.getTableName())
                    .setSecretLevel(SecretLevel.L1)
                    .setOnlineStatus(OnlineStatus.ONLINE)
                    .setTableValObj(new TableValObj()
                            .setCatalog("iceberg")
                            .setSchema("ods")
                            .setName(odsTableName)
                            .setComment("CDC ODS 统一 Schema")
                            .setColumns(List.of(
                                    new ColumnValObj().setName("_raw_json").setType("STRING").setComment("原始 Debezium JSON").setNullable(true),
                                    new ColumnValObj().setName("_op").setType("STRING").setComment("操作类型").setNullable(true),
                                    new ColumnValObj().setName("_ts").setType("LONG").setComment("变更时间戳").setNullable(true),
                                    new ColumnValObj().setName("_db").setType("STRING").setComment("源数据库").setNullable(true),
                                    new ColumnValObj().setName("_table").setType("STRING").setComment("源表名").setNullable(true),
                                    new ColumnValObj().setName("_ingestion_time").setType("TIMESTAMP").setComment("入库时间").setNullable(true)
                            ))
                    );

            metadataTableService.save(cmd);
            log.info("ODS 表创建成功: {}", odsTableName);
        } catch (SilentException e) {
            if (e.getMessage() != null && e.getMessage().contains("表已存在")) {
                log.info("ODS 表已存在，跳过创建: {}", odsTableName);
            } else {
                log.error("创建 ODS 表失败: {}", odsTableName, e);
                throw e;
            }
        } catch (Exception e) {
            log.error("创建 ODS 表失败: {}", odsTableName, e);
            throw new SilentException("创建 ODS 表失败: " + odsTableName);
        }
    }

    // ==================== 工具方法 ====================

    private List<CdcConfig> getEnabledFlinkConfigs() {
        CdcConfigListQuery query = new CdcConfigListQuery();
        query.setEnabled(true);
        query.setSyncTool(SyncTool.FLINK);
        List<CdcConfig> list = cdcConfigRepository.list(query);
        return list != null ? list : List.of();
    }

    private List<CdcConfig> getEnabledConfigsByDsAndSubject(String dsName, String subjectCode) {
        CdcConfigListQuery query = new CdcConfigListQuery();
        query.setDsName(dsName);
        if (subjectCode != null) {
            query.setSubjectCode(subjectCode);
        }
        query.setEnabled(true);
        query.setSyncTool(SyncTool.FLINK);
        List<CdcConfig> list = cdcConfigRepository.list(query);
        return list != null ? list : List.of();
    }
}
