package com.cyan.dataman.application.cdc.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.cdc.CdcConfigService;
import com.cyan.dataman.application.cdc.bo.CdcConfigBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkJobBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkTaskBO;
import com.cyan.dataman.application.cdc.cmd.CdcConfigCmd;
import com.cyan.dataman.application.cdc.cmd.CdcSparkJobCmd;
import com.cyan.dataman.application.cdc.convert.CdcAppConvert;
import com.cyan.dataman.application.cdc.job.SparkJobExecutor;
import com.cyan.dataman.application.cdc.service.CdcFlinkSyncService;
import com.cyan.dataman.application.cdc.service.DebeziumSignalService;
import com.cyan.dataman.application.ds.DsConfigService;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.domain.metadata.MetadataSubject;
import com.cyan.dataman.domain.metadata.query.MetadataSubjectFindQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataSubjectRepository;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.domain.cdc.CdcSparkTask;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.cdc.repository.CdcSparkJobRepository;
import com.cyan.dataman.domain.cdc.repository.CdcSparkTaskRepository;
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.repository.DsConfigRepository;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.TableValObj;
import com.cyan.dataman.enums.*;
import com.cyan.dataman.infra.dos.DebeziumDO;
import com.cyan.dataman.infra.rpc.request.ConnectorSaveRequest;
import com.cyan.dataman.infra.rpc.request.DebeziumRPC;
import com.cyan.dataman.infra.rpc.request.config.MySQLConnectorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * CDC 配置服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class CdcConfigServiceImpl implements CdcConfigService {

    private static final Pattern JDBC_URL_PATTERN =
            Pattern.compile("jdbc:mysql://([^:/]+):(\\d+)(?:/([^?]+))?\\??(.*)");

    @Value("${kafka.url:kafka:9092}")
    private String kafkaUrl;

    private final CdcConfigRepository cdcConfigRepository;
    private final CdcSparkJobRepository cdcSparkJobRepository;
    private final CdcSparkTaskRepository cdcSparkTaskRepository;
    private final DsConfigRepository dsConfigRepository;
    private final DebeziumRPC debeziumRpc;
    private final DebeziumSignalService debeziumSignalService;
    private final MetadataTableService metadataTableService;
    private final MetadataSubjectRepository metadataSubjectRepository;
    private final CdcFlinkSyncService cdcFlinkSyncService;
    private final SparkJobExecutor sparkJobExecutor;
    private final DsConfigService dsConfigService;

    public CdcConfigServiceImpl(CdcConfigRepository cdcConfigRepository,
                                CdcSparkJobRepository cdcSparkJobRepository,
                                CdcSparkTaskRepository cdcSparkTaskRepository,
                                DsConfigRepository dsConfigRepository,
                                DebeziumRPC debeziumRpc,
                                DebeziumSignalService debeziumSignalService,
                                MetadataTableService metadataTableService,
                                MetadataSubjectRepository metadataSubjectRepository,
                                CdcFlinkSyncService cdcFlinkSyncService,
                                SparkJobExecutor sparkJobExecutor,
                                DsConfigService dsConfigService) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcSparkJobRepository = cdcSparkJobRepository;
        this.cdcSparkTaskRepository = cdcSparkTaskRepository;
        this.dsConfigRepository = dsConfigRepository;
        this.debeziumRpc = debeziumRpc;
        this.debeziumSignalService = debeziumSignalService;
        this.metadataTableService = metadataTableService;
        this.metadataSubjectRepository = metadataSubjectRepository;
        this.cdcFlinkSyncService = cdcFlinkSyncService;
        this.sparkJobExecutor = sparkJobExecutor;
        this.dsConfigService = dsConfigService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcConfigBO create(CdcConfigCmd cmd) {
        CdcConfig existing = cdcConfigRepository.findByName(cmd.getName());
        Assert.isNull(existing, new SilentException("CDC 配置名称已存在"));

        DsConfig dsConfig = getDsConfigByName(cmd.getDsName());
        DatasourceInfo info = parseJdbcUrl(dsConfig.getUrl());

        // 如果 description 为空，从数据源获取业务表描述作为默认值
        if (com.cyan.arch.common.util.StrUtils.isBlank(cmd.getDescription())) {
            try {
                com.cyan.dataman.domain.ds.valobj.TableSchemaValObj tableSchema =
                        dsConfigService.getTableSchema(dsConfig.getName(), cmd.getDbName(), cmd.getTableName());
                if (tableSchema != null && com.cyan.arch.common.util.StrUtils.isNotBlank(tableSchema.getTableComment())) {
                    cmd.setDescription(tableSchema.getTableComment());
                }
            } catch (Exception e) {
                log.warn("获取业务表描述失败: {}.{}, error: {}", cmd.getDbName(), cmd.getTableName(), e.getMessage());
            }
        }

        // 检查该数据源是否已有 CDC 配置（共用同一个 Debezium connector）
        List<CdcConfig> datasourceConfigs = cdcConfigRepository.findByDatasource(dsConfig.getName());

        // 【新增】校验主题
        validateSubject(cmd.getSubjectCode());

        CdcConfig config = CdcAppConvert.INSTANCE.toDomain(cmd);
        config.setDsName(dsConfig.getName());

        if (datasourceConfigs.isEmpty()) {
            // 第一个表，创建新的 Debezium connector
            String connectorName = buildConnectorName(dsConfig.getName());
            int serverId = cdcConfigRepository.findNextServerId();

            config.setConnectorName(connectorName)
                    .setServerId(serverId)
                    .setRunningStatus(RunningStatus.INIT);

            config = config.save(cdcConfigRepository);
            createDebeziumConnector(config, dsConfig, info);
        } else {
            // 复用已有 connector，但新表状态必须为 INIT（确保首次同步时触发增量快照）
            CdcConfig existingConfig = datasourceConfigs.getFirst();
            config.setConnectorName(existingConfig.getConnectorName())
                    .setServerId(existingConfig.getServerId())
                    .setRunningStatus(RunningStatus.INIT);

            config = config.save(cdcConfigRepository);
            // 走完整的启动流程：更新 include.list → stop → start → 发增量快照信号
            startConnectorForTable(config);
        }

        // Flink 类型自动启动同步
        if (SyncTool.FLINK.equals(config.getSyncTool())) {
            cdcFlinkSyncService.enableCdcSync(config.getId());
        }

        return CdcAppConvert.INSTANCE.toBO(config);
    }

    @Override
    public List<CdcConfigBO> list(CdcConfigListQuery query) {
        List<CdcConfig> list = cdcConfigRepository.list(query);
        return Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAppConvert.INSTANCE::toBO)
                .toList();
    }

    @Override
    public CdcConfigBO findById(String id) {
        CdcConfig config = cdcConfigRepository.findById(id);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));
        return CdcAppConvert.INSTANCE.toBO(config);
    }

    @Override
    public CdcConfigBO findByName(String name) {
        CdcConfig config = cdcConfigRepository.findByName(name);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));
        return CdcAppConvert.INSTANCE.toBO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcConfigBO update(String id, CdcConfigCmd cmd) {
        CdcConfig config = cdcConfigRepository.findById(id);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        CdcConfig existing = cdcConfigRepository.findByName(cmd.getName());
        if (existing != null && !existing.getId().equals(id)) {
            throw new SilentException("CDC 配置名称已存在");
        }

        DsConfig dsConfig = getDsConfigByName(cmd.getDsName());
        config.setName(cmd.getName())
                .setDsName(dsConfig.getName())
                .setDbName(cmd.getDbName())
                .setTableName(cmd.getTableName())
                .setSubjectCode(cmd.getSubjectCode())
                .setIcebergTableName(cmd.getIcebergTableName())
                .setSyncTool(cmd.getSyncTool())
                .setSyncSql(cmd.getSyncSql())
                .setDescription(cmd.getDescription())
                .setSecretLevel(cmd.getSecretLevel())
                .setUpdateBy(cmd.getUpdateBy());

        config = config.update(cdcConfigRepository);
        return CdcAppConvert.INSTANCE.toBO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        CdcConfig config = cdcConfigRepository.findById(id);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        String connectorName = config.getConnectorName();

        // 先删除数据库记录
        config.delete(cdcConfigRepository);

        // 查询该数据源下剩余的配置
        List<CdcConfig> remainingConfigs = cdcConfigRepository.findByDatasource(config.getDsName());

        if (remainingConfigs.isEmpty()) {
            // 该数据源下没有其他表了，删除连接器
            if (connectorName != null) {
                try {
                    debeziumRpc.deleteConnector(connectorName);
                    log.info("删除 Debezium 连接器: {}", connectorName);
                } catch (Exception e) {
                    log.warn("删除 Debezium 连接器失败: {}", e.getMessage());
                }
            }
        } else {
            // 更新连接器的 table.include.list
            DsConfig dsConfig = getDsConfigByName(config.getDsName());
            DatasourceInfo info = parseJdbcUrl(dsConfig.getUrl());
            updateConnectorTableListFromConfigs(remainingConfigs, connectorName, dsConfig, info);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggle(String id, Boolean enabled) {
        CdcConfig config = cdcConfigRepository.findById(id);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        config.toggle(cdcConfigRepository, enabled);

        if (Boolean.TRUE.equals(enabled)) {
            // CDC 开启时创建元数据表
            createMetadataTable(config);

            if (SyncTool.FLINK.equals(config.getSyncTool())) {
                startConnectorForTable(config);
                cdcFlinkSyncService.enableCdcSync(config.getId());
            } else if (SyncTool.SPARK.equals(config.getSyncTool())) {
                triggerSparkSyncIfNeeded(config);
            }
        } else {
            if (SyncTool.FLINK.equals(config.getSyncTool())) {
                stopConnectorForTable(config);
                cdcFlinkSyncService.disableCdcSync(config.getId());
            } else if (SyncTool.SPARK.equals(config.getSyncTool())) {
                stopRunningTasks(config.getId());
            }
        }
    }

    /**
     * CDC 开启时创建对应的元数据表（ODS 层）
     * <p>
     * 从业务数据源获取表结构，转换为元数据表格式后保存。
     * 表描述默认使用 CDC 配置的 description（业务表描述），用户可在前端修改。
     */
    private void createMetadataTable(CdcConfig config) {
        try {
            // 获取业务表结构
            com.cyan.dataman.domain.ds.valobj.TableSchemaValObj tableSchema =
                    dsConfigService.getTableSchema(config.getDsName(), config.getDbName(), config.getTableName());
            if (tableSchema == null || tableSchema.getColumns() == null || tableSchema.getColumns().isEmpty()) {
                log.warn("无法获取业务表结构，跳过创建元数据表: {}.{}", config.getDbName(), config.getTableName());
                return;
            }

            // 转换字段列表
            SecretLevel secretLevel = config.getSecretLevel() != null
                    ? SecretLevel.getByCode(config.getSecretLevel())
                    : SecretLevel.L1;
            if (secretLevel == null) {
                secretLevel = SecretLevel.L1;
            }
            SecretLevel finalSecretLevel = secretLevel;
            List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> metadataColumns =
                    tableSchema.getColumns().stream()
                            .map(col -> toMetadataColumn(col, finalSecretLevel))
                            .toList();

            // 转换索引列表
            List<com.cyan.dataman.domain.metadata.valobj.IndexValObj> metadataIndexes = null;
            if (tableSchema.getIndexes() != null && !tableSchema.getIndexes().isEmpty()) {
                metadataIndexes = tableSchema.getIndexes().stream()
                        .map(idx -> new com.cyan.dataman.domain.metadata.valobj.IndexValObj()
                                .setName(idx.getName())
                                .setIndexType(idx.getIndexType())
                                .setFieldNames(idx.getFieldNames()))
                        .toList();
            }

            // 构建 TableValObj（ODS 层 Iceberg 表）
            com.cyan.dataman.domain.metadata.valobj.TableValObj tableValObj =
                    new com.cyan.dataman.domain.metadata.valobj.TableValObj()
                            .setCatalog("iceberg")
                            .setSchema(DataLayer.ODS.getCode().toLowerCase())
                            .setName(config.getIcebergTableName())
                            .setComment(config.getDescription())
                            .setColumns(metadataColumns)
                            .setIndexes(metadataIndexes);

            // 构建 MetadataTableCmd
            MetadataTableCmd cmd = new MetadataTableCmd()
                    .setName(config.getIcebergTableName())
                    .setOwner(config.getCreateBy() != null ? config.getCreateBy() : "system")
                    .setSubjectCode(config.getSubjectCode())
                    .setLayerCode(DataLayer.ODS)
                    .setComment(config.getDescription())
                    .setHeatLevel(HeatLevel.COLD)
                    .setSecretLevel(secretLevel)
                    .setOnlineStatus(OnlineStatus.ONLINE)
                    .setTableValObj(tableValObj);

            metadataTableService.save(cmd);
            log.info("CDC 开启时创建元数据表成功: {}", config.getIcebergTableName());
        } catch (Exception e) {
            log.warn("CDC 开启时创建元数据表失败: {}, error: {}", config.getIcebergTableName(), e.getMessage());
            // 不阻断 CDC 启动流程
        }
    }

    /**
     * 将数据源字段转换为元数据字段
     */
    private com.cyan.dataman.domain.metadata.valobj.ColumnValObj toMetadataColumn(
            com.cyan.dataman.domain.ds.valobj.ColumnValObj dsColumn, SecretLevel secretLevel) {
        return new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                .setName(dsColumn.getName())
                .setType(dsColumn.getType())
                .setComment(dsColumn.getComment())
                .setNullable(dsColumn.getNullable())
                .setAutoIncrement(dsColumn.getAutoIncrement())
                .setDefaultValue(dsColumn.getDefaultValue())
                .setPrecision(dsColumn.getPrecision())
                .setScale(dsColumn.getScale())
                .setSecretLevel(secretLevel);
    }

    /**
     * 触发关联的 Spark Job 执行一次
     */
    private void triggerSparkSyncIfNeeded(CdcConfig config) {
        List<CdcSparkJob> sparkJobs = cdcSparkJobRepository.findByCdcConfigId(config.getId());
        for (CdcSparkJob job : sparkJobs) {
            if (Boolean.TRUE.equals(job.getEnabled())) {
                sparkJobExecutor.executeSparkJob(job, config);
            }
        }
    }

    /**
     * 停止该 CDC 配置下所有运行中的 Spark 任务
     */
    private void stopRunningTasks(String cdcConfigId) {
        List<CdcSparkTask> runningTasks = cdcSparkTaskRepository.findRunningByCdcConfigId(cdcConfigId);
        for (CdcSparkTask task : runningTasks) {
            task.stop(cdcSparkTaskRepository);
        }
    }

    // ==================== Spark 作业配置管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcSparkJobBO createSparkJob(CdcSparkJobCmd cmd) {
        CdcConfig config = cdcConfigRepository.findById(cmd.getCdcConfigId());
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        CdcSparkJob job = CdcAppConvert.INSTANCE.toDomain(cmd);
        job = job.save(cdcSparkJobRepository);
        return CdcAppConvert.INSTANCE.toBO(job);
    }

    @Override
    public List<CdcSparkJobBO> getSparkJobsByCdcConfigId(String cdcConfigId) {
        List<CdcSparkJob> list = cdcSparkJobRepository.findByCdcConfigId(cdcConfigId);
        return Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAppConvert.INSTANCE::toBO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcSparkJobBO updateSparkJob(String id, CdcSparkJobCmd cmd) {
        CdcSparkJob job = cdcSparkJobRepository.findById(id);
        Assert.notNull(job, new SilentException("Spark 作业配置不存在"));

        job.setSyncMode(cmd.getSyncMode())
                .setCronExpression(cmd.getCronExpression())
                .setEnabled(cmd.getEnabled())
                .setUpdateBy(cmd.getUpdateBy());

        job = job.update(cdcSparkJobRepository);
        return CdcAppConvert.INSTANCE.toBO(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSparkJob(String id) {
        CdcSparkJob job = cdcSparkJobRepository.findById(id);
        Assert.notNull(job, new SilentException("Spark 作业配置不存在"));
        job.delete(cdcSparkJobRepository);
    }

    // ==================== Spark 任务实例管理 ====================

    @Override
    public List<CdcSparkTaskBO> getTaskInstances(String cdcConfigId) {
        List<CdcSparkTask> list = cdcSparkTaskRepository.findByCdcConfigId(cdcConfigId);
        return Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAppConvert.INSTANCE::toBO)
                .toList();
    }

    @Override
    public CdcSparkTaskBO getTaskInstance(String taskId) {
        CdcSparkTask task = cdcSparkTaskRepository.findById(taskId);
        Assert.notNull(task, new SilentException("任务实例不存在"));
        return CdcAppConvert.INSTANCE.toBO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopTask(String taskId) {
        CdcSparkTask task = cdcSparkTaskRepository.findById(taskId);
        Assert.notNull(task, new SilentException("任务实例不存在"));

        if (task.getStatus() == JobStatus.RUNNING) {
            task.stop(cdcSparkTaskRepository);
        }
    }

    @Override
    public CdcSparkJobBO executeSparkJob(String sparkJobId) {
        CdcSparkJob job = cdcSparkJobRepository.findById(sparkJobId);
        Assert.notNull(job, new SilentException("Spark 作业配置不存在"));

        CdcConfig config = cdcConfigRepository.findById(job.getCdcConfigId());
        Assert.notNull(config, new SilentException("关联的 CDC 配置不存在"));
        Assert.isTrue(Boolean.TRUE.equals(config.getEnabled()),
                new SilentException("CDC 配置未启用，请先启用"));

        sparkJobExecutor.executeSparkJob(job, config);
        return CdcAppConvert.INSTANCE.toBO(job);
    }

    // ==================== Debezium 连接器管理 ====================

    /**
     * 启动指定表对应的 CDC，更新连接器并启动
     * <p>
     * 快照策略：
     * - connector 不存在（数据源首次启用 / 关闭时 connector 已删除）→ 创建 connector，Debezium 自动对 include.list 中的表做全量快照
     * - connector 已存在（多表场景，其他表仍在运行）→ 更新 include.list + 发增量快照信号对该表做全量快照
     * <p>
     * 重新启用时先清空 Iceberg ODS 表数据，保证全量写入无重复。
     */
    private void startConnectorForTable(CdcConfig config) {
        DsConfig dsConfig = getDsConfigByName(config.getDsName());
        DatasourceInfo info = parseJdbcUrl(dsConfig.getUrl());
        String connectorName = config.getConnectorName();
        if (connectorName == null) {
            throw new SilentException("连接器名称未设置");
        }

        // 重新启用时，先清空 Iceberg ODS 表数据（删物理表 + 删元数据记录）
        // 后续 ensureOdsTableExists 会自动重建空表
        clearOdsTable(config);

        // 获取该数据源下所有配置
        List<CdcConfig> allConfigs = cdcConfigRepository.findByDatasource(config.getDsName());

        // 检查该 connector 是否已存在
        boolean connectorExists = checkConnectorExists(connectorName);

        if (!connectorExists) {
            // Connector 不存在（数据源首次启用 / 关闭时已删除），先创建
            createDebeziumConnector(config, dsConfig, info);
            log.info("创建并启动 Debezium 连接器: {}", connectorName);
        } else {
            // Connector 已存在（多表场景，其他表仍在运行），更新配置
            updateConnectorTableListFromConfigs(allConfigs, connectorName, dsConfig, info);
            log.info("更新 Debezium 连接器配置: {}", connectorName);

            // 更新配置后重启 connector 使新表生效。
            boolean restarted = false;
            try {
                debeziumRpc.restartConnector(connectorName);
                log.info("Debezium 连接器已重启: {}", connectorName);
                restarted = true;
            } catch (Exception e) {
                log.warn("重启 Debezium 连接器失败（将依赖 updateConnector 的自动重启）: {}", e.getMessage());
            }
            if (restarted) {
                sleepQuietly(3000);
            }
        }

        // 快照信号策略：
        // - connector 不存在 → 创建时已自动快照，不发信号
        // - connector 已存在 → 发增量快照信号（因为 Kafka topic 在关闭时被删除，需要重新全量写入）
        if (connectorExists) {
            // 等待 connector task 启动完成后再发信号
            boolean taskRunning = waitForConnectorTaskRunning(connectorName, 30);
            if (!taskRunning) {
                log.warn("Debezium 连接器 task 未能在超时时间内启动，增量快照信号可能延迟: {}", connectorName);
            }

            boolean signalSent = debeziumSignalService.sendIncrementalSnapshotSignal(
                    info.hostname(), info.port(),
                    dsConfig.getUsername(), dsConfig.getPassword(),
                    config.getDbName() + "." + config.getTableName());
            if (signalSent) {
                log.info("已触发增量快照信号（Kafka topic 重建后全量写入）: connector={}, table={}.{}", connectorName, config.getDbName(), config.getTableName());
            } else {
                log.warn("增量快照信号发送失败: connector={}, table={}.{}", connectorName, config.getDbName(), config.getTableName());
            }
        } else {
            log.info("跳过增量快照信号（新建 connector，Debezium 自动执行全量快照）: connector={}, table={}.{}", connectorName, config.getDbName(), config.getTableName());
        }

        config.setRunningStatus(RunningStatus.RUNNING);
        config.setMsg("");
        cdcConfigRepository.update(config);
    }

    /**
     * 检查 connector 是否已存在
     */
    private boolean checkConnectorExists(String connectorName) {
        try {
            List<String> connectors = debeziumRpc.connectors();
            return connectors != null && connectors.contains(connectorName);
        } catch (Exception e) {
            log.warn("检查 connector 存在性失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 等待 connector 的 task 启动并进入 RUNNING 状态
     * Debezium connector 创建/更新后，task 需要经过 rebalance 才能启动，期间会创建 Kafka topic 并开始快照
     *
     * @param connectorName  连接器名称
     * @param timeoutSeconds 最大等待秒数
     * @return task 是否成功启动
     */
    private boolean waitForConnectorTaskRunning(String connectorName, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                DebeziumDO status = debeziumRpc.connectorStatus(connectorName);
                if (status != null && status.getTasks() != null && !status.getTasks().isEmpty()) {
                    boolean allRunning = status.getTasks().stream()
                            .allMatch(t -> "RUNNING".equals(t.getState()));
                    if (allRunning) {
                        log.info("Debezium 连接器 task 已就绪: {}, task 数: {}", connectorName, status.getTasks().size());
                        return true;
                    }
                    // 如果有 task 但状态不是 RUNNING（如 FAILED），记录状态
                    status.getTasks().stream()
                            .filter(t -> !"RUNNING".equals(t.getState()))
                            .forEach(t -> log.warn("Debezium task 状态异常: connector={}, taskId={}, state={}, trace={}",
                                    connectorName, t.getId(), t.getState(), t.getTrace()));
                }
                log.info("等待 Debezium task 启动: connector={}, connectorState={}", connectorName,
                        status != null && status.getConnector() != null ? status.getConnector().getState() : "UNKNOWN");
            } catch (Exception e) {
                log.warn("查询 connector 状态失败: {}", e.getMessage());
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 停止指定表对应的 CDC
     * <p>
     * 关闭时会删除该表对应的 Kafka topic，确保重新开启时从零消费无重复数据。
     * 如果该数据源没有其他启用的表，则删除 connector（而非仅停止），以便恢复时全新创建。
     */
    private void stopConnectorForTable(CdcConfig config) {
        DsConfig dsConfig = getDsConfigByName(config.getDsName());
        DatasourceInfo info = parseJdbcUrl(dsConfig.getUrl());
        String connectorName = config.getConnectorName();
        if (connectorName == null) {
            throw new SilentException("连接器名称未设置");
        }

        // 删除该表对应的 Kafka topic（重新开启时从零消费）
        String topicName = connectorName + "." + config.getDbName() + "." + config.getTableName();
        deleteKafkaTopic(topicName);

        // 更新连接器的表列表（只保留启用的）
        updateConnectorTableListFromConfigs(
                cdcConfigRepository.findByDatasource(config.getDsName()),
                connectorName, dsConfig, info);

        // 检查该数据源下是否还有启用的表
        List<CdcConfig> enabledConfigs = cdcConfigRepository.findEnabledByDatasource(config.getDsName());
        if (enabledConfigs.isEmpty()) {
            try {
                // 删除 connector（而非仅停止），恢复时需全新创建才能保证全量快照
                debeziumRpc.deleteConnector(connectorName);
                log.info("删除 Debezium 连接器: {}", connectorName);
            } catch (Exception e) {
                log.warn("删除连接器失败: {}", e.getMessage());
            }
        }

        config.setRunningStatus(RunningStatus.STOP);
        cdcConfigRepository.update(config);
    }

    /**
     * 为 CDC 配置列表创建对应的 Kafka topic（若不存在则创建）
     * <p>
     * Topic 命名格式: {@code <topicPrefix>.<dbName>.<tableName>}
     * 如: {@code cdc-mysql-x99.cyan_databi.bi_dashboard}
     */
    private void createKafkaTopicsForConfigs(String topicPrefix, List<CdcConfig> configs) {
        if (configs.isEmpty()) {
            return;
        }
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaUrl);
        try (AdminClient admin = AdminClient.create(props)) {
            Set<String> topicsToCreate = configs.stream()
                    .map(c -> topicPrefix + "." + c.getDbName() + "." + c.getTableName())
                    .collect(Collectors.toSet());

            Set<String> existingTopics = admin.listTopics().names().get(10, TimeUnit.SECONDS);
            List<NewTopic> newTopics = topicsToCreate.stream()
                    .filter(t -> !existingTopics.contains(t))
                    .map(t -> new NewTopic(t, 1, (short) 1))
                    .toList();

            if (!newTopics.isEmpty()) {
                admin.createTopics(newTopics).all().get(10, TimeUnit.SECONDS);
                log.info("创建 Kafka topics: {}", newTopics.stream().map(NewTopic::name).toList());
            }
        } catch (Exception e) {
            log.warn("创建 Kafka topic 失败: {}", e.getMessage(), e);
            // 不阻断后续 Debezium connector 创建/更新流程
        }
    }

    /**
     * 删除指定的 Kafka topic
     * 用于 CDC 关闭时清理该表的 topic，确保重新开启时从零消费无重复数据
     */
    private void deleteKafkaTopic(String topicName) {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaUrl);
        try (AdminClient admin = AdminClient.create(props)) {
            Set<String> existingTopics = admin.listTopics().names().get(10, TimeUnit.SECONDS);
            if (existingTopics.contains(topicName)) {
                admin.deleteTopics(List.of(topicName)).all().get(10, TimeUnit.SECONDS);
                log.info("已删除 Kafka topic: {}", topicName);
            } else {
                log.info("Kafka topic 不存在，无需删除: {}", topicName);
            }
        } catch (Exception e) {
            log.warn("删除 Kafka topic 失败: {}, error: {}", topicName, e.getMessage());
        }
    }

    /**
     * 创建 Debezium 连接器
     */
    private void createDebeziumConnector(CdcConfig config, DsConfig dsConfig, DatasourceInfo info) {
        // 先创建 Kafka topic（避免 auto.create.topics.enable=false 时 Debezium 无法写入）
        createKafkaTopicsForConfigs(config.getConnectorName(), List.of(config));

        String tableIncludeList = config.getDbName() + "." + config.getTableName();
        String historyTopic = "schema-history-" + info.hostname() + "-" + info.port();

        MySQLConnectorConfig mysqlConfig = new MySQLConnectorConfig()
                .setTopicPrefix(config.getConnectorName())
                .setTaskMax("1")
                .setHostname(info.hostname())
                .setPort(info.port())
                .setUser(dsConfig.getUsername())
                .setPassword(dsConfig.getPassword())
                .setServerId(config.getServerId())
                .setDatabaseIncludeList(config.getDbName() + ",debezium_cdc")
                .setTableIncludeList(tableIncludeList)
                .setKafkaBootstrapServers(kafkaUrl)
                .setKafkaTopic(historyTopic)
                .setIncludeSchemaChanges(true)
                .setSnapshotMode("when_needed")
                .setSignalDataCollection("debezium_cdc.signal")
                .setIncrementalSnapshotEnabled(true)
                .setIncrementalSnapshotChunkSize("1024");

        ConnectorSaveRequest request = new ConnectorSaveRequest(config.getConnectorName(), mysqlConfig);
        debeziumRpc.createConnector(request);

        // 确保信号表存在
        debeziumSignalService.ensureSignalTableExists(info.hostname(), info.port(), dsConfig.getUsername(), dsConfig.getPassword());

        log.info("创建 Debezium 连接器: {}, 表: {}", config.getConnectorName(), tableIncludeList);
    }

    /**
     * 更新连接器的 table.include.list
     */
    private void updateConnectorTableList(String dsName, String connectorName, DsConfig dsConfig, DatasourceInfo info) {
        List<CdcConfig> allConfigs = cdcConfigRepository.findByDatasource(dsName);
        updateConnectorTableListFromConfigs(allConfigs, connectorName, dsConfig, info);
    }

    /**
     * 根据配置列表更新连接器的表列表（只包含已启用的配置）
     */
    private void updateConnectorTableListFromConfigs(List<CdcConfig> allConfigs, String connectorName,
                                                     DsConfig dsConfig, DatasourceInfo info) {
        // 只包含启用状态的表
        List<CdcConfig> enabledConfigs = allConfigs.stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .toList();

        // 获取 serverId（从任一配置中获取，它们共享同一个 connector）
        Integer serverId = enabledConfigs.isEmpty() ? null : enabledConfigs.getFirst().getServerId();

        if (enabledConfigs.isEmpty()) {
            // 没有启用的表，设置空列表
            updateConnectorConfig(connectorName, info, dsConfig, "", serverId);
            return;
        }

        String tableIncludeList = enabledConfigs.stream()
                .map(c -> c.getDbName() + "." + c.getTableName())
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));

        // 先创建 Kafka topic（避免 auto.create.topics.enable=false 时 Debezium 无法写入）
        createKafkaTopicsForConfigs(connectorName, enabledConfigs);

        updateConnectorConfig(connectorName, info, dsConfig, tableIncludeList, serverId);
    }

    /**
     * 更新单个连接器配置
     */
    private void updateConnectorConfig(String connectorName, DatasourceInfo info,
                                       DsConfig dsConfig, String tableIncludeList, Integer serverId) {
        String historyTopic = "schema-history-" + info.hostname() + "-" + info.port();
        // 从 table.include.list 中提取所有唯一的数据库名
        String databaseIncludeList = tableIncludeList.isEmpty() ? "" :
                Arrays.stream(tableIncludeList.split(","))
                        .map(t -> t.split("\\.")[0])
                        .distinct()
                        .collect(Collectors.joining(","));
        if (!databaseIncludeList.contains("debezium_cdc")) {
            databaseIncludeList = databaseIncludeList.isEmpty() ? "debezium_cdc" : databaseIncludeList + ",debezium_cdc";
        }
        MySQLConnectorConfig mysqlConfig = new MySQLConnectorConfig()
                .setTopicPrefix(connectorName)
                .setTaskMax("1")
                .setHostname(info.hostname())
                .setPort(info.port())
                .setUser(dsConfig.getUsername())
                .setPassword(dsConfig.getPassword())
                .setServerId(serverId)
                .setDatabaseIncludeList(databaseIncludeList)
                .setTableIncludeList(tableIncludeList)
                .setKafkaBootstrapServers(kafkaUrl)
                .setKafkaTopic(historyTopic)
                .setIncludeSchemaChanges(true)
                .setSnapshotMode("when_needed")
                .setSignalDataCollection("debezium_cdc.signal")
                .setIncrementalSnapshotEnabled(true)
                .setIncrementalSnapshotChunkSize("1024");

        debeziumRpc.updateConnector(connectorName, mysqlConfig);
    }

    // ==================== 工具方法 ====================

    private DsConfig getDsConfigByName(String dsName) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源配置不存在: " + dsName));
        return dsConfig;
    }

    private String buildConnectorName(String dsName) {
        return "cdc-" + dsName;
    }

    /**
     * 静默睡眠指定毫秒数，不抛异常
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 校验主题编码是否有效
     *
     * @param subjectCode 主题编码
     */
    private void validateSubject(String subjectCode) {
        Assert.notBlank(subjectCode, new SilentException("主题编码不能为空"));

        MetadataSubject subject = metadataSubjectRepository
                .find(new MetadataSubjectFindQuery().setSubjectCode(subjectCode));
        Assert.notNull(subject, new SilentException("主题不存在: " + subjectCode));
    }

    /**
     * 为 FLINK CDC 确保 Iceberg 表存在 op 字段（方案 B 已废弃，保留代码供参考）
     *
     * @param icebergTableName Iceberg 表名（可能是 schema.tableName 或纯表名）
     */
    private void ensureOpColumnForFlinkCdc(String icebergTableName) {
        String tableName;

        // 解析 icebergTableName，支持 "schema.tableName" 或纯表名格式
        if (icebergTableName.contains(".")) {
            String[] parts = icebergTableName.split("\\.");
            if (parts.length == 2) {
                tableName = parts[1];
            } else {
                // 多个点的情况，取最后两部分
                tableName = parts[parts.length - 1];
            }
        } else {
            tableName = icebergTableName;
        }

        // 查找元数据表
        MetadataTableBO metadata = metadataTableService.findOne(new MetadataTableOneQuery().setName(tableName));
        if (metadata == null) {
            throw new SilentException("Iceberg 表不存在: " + icebergTableName);
        }

        // FLINK CDC 必须使用 ODS 层表，检查表是否在 ODS 层
        // 使用 metadata.table.schema 作为 schema 进行验证
        String actualSchema = metadata.getTable() != null ? metadata.getTable().getSchema() : metadata.getLayerCode();
        validateOdsLayer(actualSchema, tableName);

        // 检查是否已存在 op 字段
        boolean opColumnExists = metadata.getTable() != null
                && metadata.getTable().getColumns() != null
                && metadata.getTable().getColumns().stream()
                .anyMatch(col -> "op".equals(col.getName()));

        if (opColumnExists) {
            log.info("Iceberg 表 {}.{} 已存在 op 字段", actualSchema, tableName);
            return;
        }

        // 通过 MetadataTableService.update 添加 op 字段
        TableValObj table = metadata.getTable();
        // 构建新的列 表 + op 列
        ColumnValObj opColumn = new ColumnValObj()
                .setName("op")
                .setType("STRING")
                .setComment("CDC operation type: c(create), r(read), u(update), d(delete)")
                .setSecretLevel(SecretLevel.L1)
                .setNullable(true)
                .setAutoIncrement(false);
        table.getColumns().add(opColumn);

        // 构建 MetadataTableCmd
        MetadataTableCmd cmd = new MetadataTableCmd();
        cmd.setName(metadata.getName());
        cmd.setOwner(metadata.getOwner());
        cmd.setSubjectCode(metadata.getSubjectCode());
        cmd.setLayerCode(DataLayer.valueOf(metadata.getLayerCode().toUpperCase()));
        cmd.setComment(metadata.getComment());
        cmd.setHeatLevel(metadata.getHeatLevel());
        cmd.setSecretLevel(metadata.getSecretLevel());
        cmd.setOnlineStatus(metadata.getOnlineStatus());
        cmd.setTableValObj(table);

        metadataTableService.update(metadata.getId(), cmd);
        log.info("成功为 Iceberg 表 {}.{} 添加 op 字段", actualSchema, tableName);
    }

    /**
     * 验证 Iceberg 表是否在 ODS 层
     *
     * @param schema    库名
     * @param tableName 表名
     */
    private void validateOdsLayer(String schema, String tableName) {
        MetadataTableOneQuery query = new MetadataTableOneQuery();
        query.setName(tableName);
        MetadataTableBO metadataTable = metadataTableService.findOne(query);

        if (metadataTable == null) {
            throw new SilentException("Iceberg 表不存在: " + schema + "." + tableName);
        }

        if (!DataLayer.ODS.getCode().equalsIgnoreCase(metadataTable.getLayerCode())) {
            throw new SilentException("Flink CDC 同步目标表必须位于 ODS 层，当前表位于: " +
                    (metadataTable.getLayerCode() != null ? metadataTable.getLayerCode() : "未知层"));
        }
    }

    private DatasourceInfo parseJdbcUrl(String jdbcUrl) {
        Matcher matcher = JDBC_URL_PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new SilentException("无法解析 JDBC URL: " + jdbcUrl);
        }
        return new DatasourceInfo(matcher.group(1), matcher.group(2));
    }

    /**
     * 清空 Iceberg ODS 表数据（删物理表 + 删元数据记录）
     * 后续 ensureOdsTableExists 会自动重建空表
     */
    private void clearOdsTable(CdcConfig config) {
        String safeSubject = safeName(config.getSubjectCode());
        String safeDb = safeName(config.getDbName());
        String safeTable = safeName(config.getTableName());
        String odsTableName = "ods_cdc_raw_" + safeSubject + "_" + safeDb + "_" + safeTable;

        MetadataTableBO odsTable = metadataTableService.findOne(new MetadataTableOneQuery().setName(odsTableName));
        if (odsTable != null) {
            try {
                metadataTableService.delete(odsTable.getId());
                log.info("已删除 Iceberg ODS 表（重新启用时将重建空表）: {}", odsTableName);
            } catch (Exception e) {
                log.warn("删除 Iceberg ODS 表失败（后续 ensureOdsTableExists 将重建）: {}, error: {}", odsTableName, e.getMessage());
            }
        } else {
            log.info("Iceberg ODS 表不存在，无需清空: {}", odsTableName);
        }
    }

    private String safeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private record DatasourceInfo(String hostname, String port) {
    }
}
