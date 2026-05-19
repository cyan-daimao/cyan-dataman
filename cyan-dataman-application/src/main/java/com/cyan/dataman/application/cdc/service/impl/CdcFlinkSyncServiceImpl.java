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
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.repository.DsConfigRepository;
import com.cyan.dataman.domain.ds.valobj.ColumnValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.enums.*;
import com.cyan.dataman.infra.util.DebeziumTypeMapper;
import com.cyan.dataman.infra.util.DsJdbcUtil;
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
import java.util.stream.Collectors;

/**
 * CDC Flink 同步服务实现（Application 模式）
 * <p>
 * 通过 Flink Kubernetes Operator 管理 FlinkDeployment CR。
 * 一表对应一个 FlinkDeployment，每个作业包含：
 * 一个 Kafka Source（单 topic）+ 一个 Iceberg ODS Sink。
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
    private final DsConfigRepository dsConfigRepository;
    private final DsJdbcUtil dsJdbcUtil;
    private final KubernetesClient k8sClient;

    public CdcFlinkSyncServiceImpl(CdcConfigRepository cdcConfigRepository,
                                   CdcFlinkJobRepository cdcFlinkJobRepository,
                                   MetadataTableService metadataTableService,
                                   DsConfigRepository dsConfigRepository,
                                   DsJdbcUtil dsJdbcUtil) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcFlinkJobRepository = cdcFlinkJobRepository;
        this.metadataTableService = metadataTableService;
        this.dsConfigRepository = dsConfigRepository;
        this.dsJdbcUtil = dsJdbcUtil;
        this.k8sClient = new KubernetesClientBuilder().build();
    }

    @Override
    public void startFlinkSyncJob() {
        List<CdcConfig> enabledConfigs = getEnabledFlinkConfigs();
        for (CdcConfig config : enabledConfigs) {
            String deploymentName = getDeploymentName(config.getDsName(), config.getDbName(), config.getTableName());
            if (getFlinkDeployment(deploymentName) != null) {
                log.info("表 {}.{} 已有 FlinkDeployment，跳过提交", config.getDbName(), config.getTableName());
                continue;
            }
            submitFlinkApplication(config);
        }
    }

    @Override
    public void stopFlinkSyncJob() {
        List<CdcFlinkJob> runningJobs = cdcFlinkJobRepository.findAllRunning();
        for (CdcFlinkJob job : runningJobs) {
            String deploymentName = getDeploymentName(job.getDsName(), job.getDbName(), job.getTableName());
            deleteFlinkApplication(deploymentName);
            log.info("已删除 FlinkDeployment: {}", deploymentName);
        }
    }

    @Override
    public void enableCdcSync(String cdcConfigId) {
        log.info("enableCdcSync 开始, cdcConfigId={}", cdcConfigId);
        CdcConfig config = cdcConfigRepository.findById(cdcConfigId);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));
        Assert.isTrue(SyncTool.FLINK.equals(config.getSyncTool()),
                new SilentException("该 CDC 配置不是 FLINK 类型"));

        if (!Boolean.TRUE.equals(config.getEnabled())) {
            config.toggle(cdcConfigRepository, true);
        }

        String deploymentName = getDeploymentName(config.getDsName(), config.getDbName(), config.getTableName());
        String configMapName = getConfigMapName(config.getDsName(), config.getDbName(), config.getTableName());

        log.info("确保 ODS 表存在, deploymentName={}, table={}.{}", deploymentName, config.getDbName(), config.getTableName());
        try {
            ensureOdsTableExists(config);
            log.info("ODS 表确保完成, table={}.{}", config.getDbName(), config.getTableName());
        } catch (Exception e) {
            log.error("ensureOdsTableExists 异常, table={}.{}: {}", config.getDbName(), config.getTableName(), e.getMessage(), e);
            throw new SilentException("创建 ODS 元数据表失败: " + config.getDbName() + "." + config.getTableName());
        }

        GenericKubernetesResource deployment = getFlinkDeployment(deploymentName);
        log.info("FlinkDeployment 状态, deploymentName={}, exists={}", deploymentName, deployment != null);

        if (deployment == null) {
            submitFlinkApplication(config);
        } else {
            CdcFlinkJob job = cdcFlinkJobRepository.findByDsNameAndDbNameAndTableName(
                    config.getDsName(), config.getDbName(), config.getTableName());
            if (job == null) {
                log.warn("FlinkDeployment 存在但数据库中无对应 FlinkJob 记录, 重建记录, deploymentName={}", deploymentName);
                String sql = buildFlinkSql(config);
                createOrUpdateConfigMap(configMapName, sql);
                saveFlinkJobRecord(config, sql, JobStatus.RUNNING, "");
            } else {
                log.info("FlinkDeployment 和 FlinkJob 记录均已存在, 跳过: {}", deploymentName);
            }
        }
        log.info("enableCdcSync 结束, cdcConfigId={}", cdcConfigId);
    }

    @Override
    public void disableCdcSync(String cdcConfigId) {
        CdcConfig config = cdcConfigRepository.findById(cdcConfigId);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        if (Boolean.TRUE.equals(config.getEnabled())) {
            config.toggle(cdcConfigRepository, false);
        }

        String deploymentName = getDeploymentName(config.getDsName(), config.getDbName(), config.getTableName());
        deleteFlinkApplication(deploymentName);

        CdcFlinkJob job = cdcFlinkJobRepository.findByDsNameAndDbNameAndTableName(
                config.getDsName(), config.getDbName(), config.getTableName());
        if (job != null) {
            job.stop(cdcFlinkJobRepository);
        }
        log.info("已禁用 CDC 同步并删除 FlinkDeployment, table={}.{}", config.getDbName(), config.getTableName());
    }

    @Override
    public void cancelFlinkJob(String flinkJobId) {
        CdcFlinkJob flinkJob = cdcFlinkJobRepository.findByFlinkJobId(flinkJobId);
        if (flinkJob == null) {
            log.warn("Flink 作业不存在，flinkJobId: {}", flinkJobId);
            return;
        }

        String deploymentName = getDeploymentName(flinkJob.getDsName(), flinkJob.getDbName(), flinkJob.getTableName());
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
                String deploymentName = getDeploymentName(job.getDsName(), job.getDbName(), job.getTableName());
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
        List<CdcConfig> configs = getEnabledFlinkConfigs();
        for (CdcConfig config : configs) {
            if (dsName.equals(config.getDsName())) {
                return cdcFlinkJobRepository.findByDsNameAndDbNameAndTableName(
                        config.getDsName(), config.getDbName(), config.getTableName());
            }
        }
        return null;
    }

    @Override
    public void restartFlinkJob(String cdcConfigId) {
        CdcConfig config = cdcConfigRepository.findById(cdcConfigId);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        String deploymentName = getDeploymentName(config.getDsName(), config.getDbName(), config.getTableName());
        log.info("开始重启 Flink CDC 作业: {}.{}.{}", config.getDsName(), config.getDbName(), config.getTableName());

        // 1. 删除旧 Deployment
        deleteFlinkApplication(deploymentName);

        // 2. 等待 K8s 删除完成（最多 30 秒）
        int retries = 30;
        while (retries-- > 0) {
            if (getFlinkDeployment(deploymentName) == null) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 3. 重新提交
        submitFlinkApplication(config);
        log.info("Flink CDC 作业重启完成: {}.{}.{}", config.getDsName(), config.getDbName(), config.getTableName());
    }

    // ==================== Application 模式作业提交 ====================

    private void submitFlinkApplication(CdcConfig config) {
        String sql = buildFlinkSql(config);
        String deploymentName = getDeploymentName(config.getDsName(), config.getDbName(), config.getTableName());
        String configMapName = getConfigMapName(config.getDsName(), config.getDbName(), config.getTableName());

        try {
            ensureOdsTableExists(config);
            createOrUpdateConfigMap(configMapName, sql);
            log.info("ConfigMap 创建/更新成功: {}", configMapName);

            String yaml = buildFlinkDeploymentYaml(deploymentName, configMapName);
            k8sClient.load(new java.io.ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)))
                    .inNamespace(flinkNamespace)
                    .createOrReplace();
            log.info("FlinkDeployment 创建/更新成功: {}", deploymentName);

            saveFlinkJobRecord(config, sql, JobStatus.RUNNING, "");
            log.info("Flink CDC 作业已提交, table={}.{}", config.getDbName(), config.getTableName());
        } catch (Exception e) {
            log.error("Flink CDC 作业提交失败, table={}.{}", config.getDbName(), config.getTableName(), e);
            saveFlinkJobRecord(config, sql, JobStatus.FAILED, e.getMessage());
            throw new SilentException("Flink CDC 作业提交失败: " + config.getDbName() + "." + config.getTableName());
        }
    }

    private void saveFlinkJobRecord(CdcConfig config, String sql, JobStatus status, String errorMsg) {
        CdcFlinkJob job = cdcFlinkJobRepository.findByDsNameAndDbNameAndTableName(
                config.getDsName(), config.getDbName(), config.getTableName());
        if (job == null) {
            job = new CdcFlinkJob()
                    .setDsName(config.getDsName())
                    .setDbName(config.getDbName())
                    .setTableName(config.getTableName())
                    .setSubjectCode(config.getSubjectCode())
                    .setFlinkJobId(getDeploymentName(config.getDsName(), config.getDbName(), config.getTableName()))
                    .setFlinkSql(sql)
                    .setEnabled(true)
                    .setStatus(status)
                    .setErrorMessage(errorMsg)
                    .setCreatedAt(LocalDateTime.now())
                    .setUpdatedAt(LocalDateTime.now())
                    .setCreateBy(config.getCreateBy())
                    .setUpdateBy(config.getUpdateBy());
            job.save(cdcFlinkJobRepository);
        } else {
            job.setFlinkSql(sql)
                    .setEnabled(true)
                    .setStatus(status)
                    .setErrorMessage(errorMsg)
                    .setUpdatedAt(LocalDateTime.now())
                    .setUpdateBy(config.getUpdateBy());
            job.update(cdcFlinkJobRepository);
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
                      memory: "1g"
                      cpu: 0.5
                  taskManager:
                    resource:
                      memory: "1g"
                      cpu: 0.5
                  flinkConfiguration:
                    state.backend.type: rocksdb
                    classloader.parent-first-patterns.additional: com.codahale.metrics
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
                    jarURI: local:///opt/flink/lib/sql-runner.jar
                    entryClass: com.cyan.dataman.infra.flink.SqlRunner
                    args:
                      - "/opt/flink/sql/job.sql"
                    parallelism: 1
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

    private String getDeploymentName(String dsName, String dbName, String tableName) {
        return "cdc-" + toK8sName(dsName) + "-" + toK8sName(dbName) + "-" + toK8sName(tableName);
    }

    private String getConfigMapName(String dsName, String dbName, String tableName) {
        return getDeploymentName(dsName, dbName, tableName) + "-sql";
    }

    // ==================== SQL 生成 ====================

    private String buildFlinkSql(CdcConfig config) {
        String safeDsName = toK8sName(config.getDsName());
        String topicName = "cdc-" + config.getDsName() + "." + config.getDbName() + "." + config.getTableName();
        String groupId = "flink-cdc-" + safeDsName + "-" + toK8sName(config.getDbName()) + "-" + toK8sName(config.getTableName());

        List<ColumnValObj> columns = fetchSourceColumns(config);
        String sinkSql = buildSinkSql(config, columns);

        return String.format("""
                -- 创建 Iceberg REST Catalog（复用 Gravitino）
                CREATE CATALOG IF NOT EXISTS rest WITH (
                  'type' = 'iceberg',
                  'catalog-type' = 'rest',
                  'uri' = '%s',
                  's3.endpoint' = '%s',
                  's3.access-key-id' = '%s',
                  's3.secret-access-key' = '%s',
                  's3.path-style-access' = 'true'
                );

                -- Kafka Source：单 topic，raw format 读取完整 Debezium JSON
                CREATE TABLE IF NOT EXISTS kafka_source (
                  _raw_json STRING
                ) WITH (
                  'connector' = 'kafka',
                  'topic' = '%s',
                  'properties.bootstrap.servers' = '%s',
                  'properties.group.id' = '%s',
                  'scan.startup.mode' = 'earliest-offset',
                  'format' = 'raw'
                );

                %s
                """, icebergRestUri, rustfsEndpoint, rustfsAccessKey, rustfsSecretKey,
                topicName, kafkaBootstrapServers, groupId, sinkSql);
    }

    private String buildSinkSql(CdcConfig config, List<ColumnValObj> columns) {
        String safeSubject = safeName(config.getSubjectCode());
        String safeDb = safeName(config.getDbName());
        String safeTable = safeName(config.getTableName());
        String odsTableName = "ods_cdc_raw_" + safeSubject + "_" + safeDb + "_" + safeTable;
        String fullTableName = "rest.ods." + odsTableName;

        Set<String> sourceColNames = columns.stream()
                .map(ColumnValObj::getName)
                .collect(Collectors.toSet());

        // 构建业务字段 DDL
        StringBuilder colDdl = new StringBuilder();
        for (ColumnValObj col : columns) {
            String flinkType = DebeziumTypeMapper.toFlinkSqlType(col);
            colDdl.append("  `").append(col.getName()).append("` ").append(flinkType).append(",\n");
        }
        if (!sourceColNames.contains("_op")) {
            colDdl.append("  `_op` STRING,\n");
        }
        if (!sourceColNames.contains("_ts")) {
            colDdl.append("  `_ts` BIGINT,\n");
        }
        if (!sourceColNames.contains("_db")) {
            colDdl.append("  `_db` STRING,\n");
        }
        if (!sourceColNames.contains("_table")) {
            colDdl.append("  `_table` STRING,\n");
        }
        if (!sourceColNames.contains("_ingestion_time")) {
            colDdl.append("  `_ingestion_time` TIMESTAMP_LTZ(3),\n");
        }

        // 构建业务字段提取表达式
        StringBuilder colExtract = new StringBuilder();
        for (ColumnValObj col : columns) {
            String flinkType = DebeziumTypeMapper.toFlinkSqlType(col);
            colExtract.append("  ").append(DebeziumTypeMapper.buildExtractExpr(col.getName(), flinkType)).append(",\n");
        }
        if (!sourceColNames.contains("_op")) {
            colExtract.append("  COALESCE(JSON_VALUE(_raw_json, '$.payload.op'), JSON_VALUE(_raw_json, '$.op')) AS `_op`,\n");
        }
        if (!sourceColNames.contains("_ts")) {
            colExtract.append("  CAST(COALESCE(JSON_VALUE(_raw_json, '$.payload.ts_ms'), JSON_VALUE(_raw_json, '$.ts_ms')) AS BIGINT) AS `_ts`,\n");
        }
        if (!sourceColNames.contains("_db")) {
            colExtract.append("  COALESCE(JSON_VALUE(_raw_json, '$.payload.source.db'), JSON_VALUE(_raw_json, '$.source.db')) AS `_db`,\n");
        }
        if (!sourceColNames.contains("_table")) {
            colExtract.append("  COALESCE(JSON_VALUE(_raw_json, '$.payload.source.table'), JSON_VALUE(_raw_json, '$.source.table')) AS `_table`,\n");
        }
        if (!sourceColNames.contains("_ingestion_time")) {
            colExtract.append("  NOW() AS `_ingestion_time`,\n");
        }

        String colDdlStr = stripTrailingComma(colDdl.toString());
        String colExtractStr = stripTrailingComma(colExtract.toString());

        return String.format("""
                -- Sink: %s.%s
                CREATE TABLE IF NOT EXISTS %s (
                %s);

                INSERT INTO %s
                SELECT
                %s FROM kafka_source;
                """,
                config.getDbName(), config.getTableName(),
                fullTableName, colDdlStr,
                fullTableName, colExtractStr);
    }

    private String safeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * 将名称转换为 RFC 1123 兼容格式（用于 K8s 资源命名：Deployment、ConfigMap 等）
     * RFC 1123 要求：小写字母、数字、'-' 或 '.'，首尾必须是字母数字
     */
    private String toK8sName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }

    private String stripTrailingComma(String s) {
        if (s != null && s.endsWith(",\n")) {
            return s.substring(0, s.length() - 2) + "\n";
        }
        return s;
    }

    /**
     * 确保 ODS 表已存在（通过元数据平台创建，Flink 只负责写入）
     */
    private void ensureOdsTableExists(CdcConfig config) {
        String safeSubject = safeName(config.getSubjectCode());
        String safeDb = safeName(config.getDbName());
        String safeTable = safeName(config.getTableName());
        String odsTableName = "ods_cdc_raw_" + safeSubject + "_" + safeDb + "_" + safeTable;

        List<ColumnValObj> sourceColumns = fetchSourceColumns(config);
        Set<String> sourceColNames = sourceColumns.stream()
                .map(ColumnValObj::getName)
                .collect(Collectors.toSet());
        log.info("源表 {}.{} 共 {} 个字段: {}", config.getDbName(), config.getTableName(),
                sourceColumns.size(), sourceColNames);

        List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> odsColumns = new ArrayList<>();
        for (ColumnValObj sourceCol : sourceColumns) {
            com.cyan.dataman.domain.metadata.valobj.ColumnValObj col =
                    new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                            .setName(sourceCol.getName())
                            .setType(sourceCol.getType())
                            .setComment(sourceCol.getComment())
                            .setNullable(true)
                            .setPrecision(sourceCol.getPrecision())
                            .setScale(sourceCol.getScale());
            odsColumns.add(col);
        }

        if (!sourceColNames.contains("_op")) {
            odsColumns.add(new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                    .setName("_op").setType("STRING").setComment("操作类型").setNullable(true));
        }
        if (!sourceColNames.contains("_ts")) {
            odsColumns.add(new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                    .setName("_ts").setType("LONG").setComment("变更时间戳").setNullable(true));
        }
        if (!sourceColNames.contains("_db")) {
            odsColumns.add(new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                    .setName("_db").setType("STRING").setComment("源数据库").setNullable(true));
        }
        if (!sourceColNames.contains("_table")) {
            odsColumns.add(new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                    .setName("_table").setType("STRING").setComment("源表名").setNullable(true));
        }
        if (!sourceColNames.contains("_ingestion_time")) {
            odsColumns.add(new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                    .setName("_ingestion_time").setType("TIMESTAMP_TZ").setComment("入库时间").setNullable(true));
        }

        List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> uniqueOdsColumns = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (com.cyan.dataman.domain.metadata.valobj.ColumnValObj col : odsColumns) {
            if (seen.add(col.getName())) {
                uniqueOdsColumns.add(col);
            } else {
                log.warn("ODS 表 {} 出现重复字段名 [{}]，已去重", odsTableName, col.getName());
            }
        }
        odsColumns = uniqueOdsColumns;
        log.info("ODS 表 {} 最终字段列表 ({} 个): {}", odsTableName, odsColumns.size(),
                odsColumns.stream().map(com.cyan.dataman.domain.metadata.valobj.ColumnValObj::getName).toList());

        try {
            MetadataTableCmd cmd = new MetadataTableCmd()
                    .setName(odsTableName)
                    .setOwner(config.getCreateBy())
                    .setSubjectCode(config.getSubjectCode())
                    .setLayerCode(DataLayer.ODS)
                    .setComment("CDC ODS 表: " + config.getDbName() + "." + config.getTableName())
                    .setSecretLevel(SecretLevel.L1)
                    .setOnlineStatus(OnlineStatus.ONLINE)
                    .setTableValObj(new com.cyan.dataman.domain.metadata.valobj.TableValObj()
                            .setCatalog("iceberg")
                            .setSchema("ods")
                            .setName(odsTableName)
                            .setComment("CDC ODS 统一 Schema")
                            .setColumns(odsColumns)
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

    private List<ColumnValObj> fetchSourceColumns(CdcConfig config) {
        try {
            DsConfig dsConfig = dsConfigRepository.findByName(config.getDsName());
            if (dsConfig == null) {
                log.warn("数据源不存在: {}", config.getDsName());
                return List.of();
            }
            TableSchemaValObj schema = dsJdbcUtil.getTableSchema(dsConfig, config.getDbName(), config.getTableName());
            return schema.getColumns() != null ? schema.getColumns() : List.of();
        } catch (Exception e) {
            log.error("获取源表结构失败: {}.{}", config.getDbName(), config.getTableName(), e);
            return List.of();
        }
    }

    private List<CdcConfig> getEnabledFlinkConfigs() {
        CdcConfigListQuery query = new CdcConfigListQuery();
        query.setEnabled(true);
        query.setSyncTool(SyncTool.FLINK);
        List<CdcConfig> list = cdcConfigRepository.list(query);
        return list != null ? list : List.of();
    }
}
