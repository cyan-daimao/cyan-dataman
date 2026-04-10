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
import com.cyan.dataman.application.cdc.service.DebeziumSignalService;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.domain.cdc.CdcSparkTask;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.cdc.repository.CdcSparkJobRepository;
import com.cyan.dataman.domain.cdc.repository.CdcSparkTaskRepository;
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.repository.DsConfigRepository;
import com.cyan.dataman.enums.JobStatus;
import com.cyan.dataman.enums.RunningStatus;
import com.cyan.dataman.infra.rpc.request.ConnectorSaveRequest;
import com.cyan.dataman.infra.rpc.request.DebeziumRPC;
import com.cyan.dataman.infra.rpc.request.config.MySQLConnectorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public CdcConfigServiceImpl(CdcConfigRepository cdcConfigRepository,
                                CdcSparkJobRepository cdcSparkJobRepository,
                                CdcSparkTaskRepository cdcSparkTaskRepository,
                                DsConfigRepository dsConfigRepository,
                                DebeziumRPC debeziumRpc,
                                DebeziumSignalService debeziumSignalService) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcSparkJobRepository = cdcSparkJobRepository;
        this.cdcSparkTaskRepository = cdcSparkTaskRepository;
        this.dsConfigRepository = dsConfigRepository;
        this.debeziumRpc = debeziumRpc;
        this.debeziumSignalService = debeziumSignalService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcConfigBO create(CdcConfigCmd cmd) {
        CdcConfig existing = cdcConfigRepository.findByName(cmd.getName());
        Assert.isNull(existing, new SilentException("CDC 配置名称已存在"));

        DsConfig dsConfig = getDsConfigByName(cmd.getDsName());
        DatasourceInfo info = parseJdbcUrl(dsConfig.getUrl());

        // 检查该数据源是否已有 CDC 配置（共用同一个 Debezium connector）
        List<CdcConfig> datasourceConfigs = cdcConfigRepository.findByDatasource(dsConfig.getName());

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

            // 创建 Debezium 连接器
            createDebeziumConnector(config, dsConfig, info);
        } else {
            // 复用已有 connector，更新其 table.include.list
            CdcConfig existingConfig = datasourceConfigs.getFirst();
            config.setConnectorName(existingConfig.getConnectorName())
                    .setServerId(existingConfig.getServerId())
                    .setRunningStatus(existingConfig.getRunningStatus());

            config = config.save(cdcConfigRepository);

            // 更新连接器的表列表
            updateConnectorTableList(dsConfig.getName(), existingConfig.getConnectorName(), dsConfig, info);
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
                .setIcebergTableName(cmd.getIcebergTableName())
                .setSyncTool(cmd.getSyncTool())
                .setSyncSql(cmd.getSyncSql())
                .setDescription(cmd.getDescription())
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
            startConnectorForTable(config);
        } else {
            stopConnectorForTable(config);
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
                .setSparkSql(cmd.getSparkSql())
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

    // ==================== Debezium 连接器管理 ====================

    /**
     * 启动指定表对应的 CDC，更新连接器并启动
     */
    private void startConnectorForTable(CdcConfig config) {
        DsConfig dsConfig = getDsConfigByName(config.getDsName());
        DatasourceInfo info = parseJdbcUrl(dsConfig.getUrl());
        String connectorName = config.getConnectorName();
        if (connectorName == null) {
            throw new SilentException("连接器名称未设置");
        }

        // 获取该数据源下所有配置
        List<CdcConfig> allConfigs = cdcConfigRepository.findByDatasource(config.getDsName());
        boolean isNewTable = !Boolean.TRUE.equals(config.getEnabled());

        // 更新连接器配置（包含已启用的表）
        updateConnectorTableListFromConfigs(allConfigs, connectorName, dsConfig, info);

        if (isNewTable) {
            // 新增表：先停止再启动，让 Debezium 获取新表 schema
            try {
                debeziumRpc.stopConnector(connectorName);
                Thread.sleep(2000);
            } catch (Exception e) {
                log.warn("停止连接器失败（可忽略）: {}", e.getMessage());
            }

            try {
                debeziumRpc.startConnector(connectorName);
                config.setRunningStatus(RunningStatus.RUNNING);
                cdcConfigRepository.update(config);
                log.info("启动 Debezium 连接器: {}", connectorName);
            } catch (Exception e) {
                config.setRunningStatus(RunningStatus.ERROR);
                config.setMsg("启动连接器失败: " + e.getMessage());
                cdcConfigRepository.update(config);
                throw new SilentException("启动连接器失败: " + e.getMessage());
            }
        } else {
            try {
                debeziumRpc.startConnector(connectorName);
                config.setRunningStatus(RunningStatus.RUNNING);
                cdcConfigRepository.update(config);
            } catch (Exception e) {
                log.warn("启动连接器失败（可能已在运行）: {}", e.getMessage());
            }
        }
    }

    /**
     * 停止指定表对应的 CDC
     */
    private void stopConnectorForTable(CdcConfig config) {
        DsConfig dsConfig = getDsConfigByName(config.getDsName());
        DatasourceInfo info = parseJdbcUrl(dsConfig.getUrl());
        String connectorName = config.getConnectorName();
        if (connectorName == null) {
            throw new SilentException("连接器名称未设置");
        }

        // 更新连接器的表列表（只保留启用的）
        updateConnectorTableListFromConfigs(
                cdcConfigRepository.findByDatasource(config.getDsName()),
                connectorName, dsConfig, info);

        // 检查该数据源下是否还有启用的表
        List<CdcConfig> enabledConfigs = cdcConfigRepository.findEnabledByDatasource(config.getDsName());
        if (enabledConfigs.isEmpty()) {
            try {
                debeziumRpc.stopConnector(connectorName);
                config.setRunningStatus(RunningStatus.STOP);
                cdcConfigRepository.update(config);
                log.info("停止 Debezium 连接器: {}", connectorName);
            } catch (Exception e) {
                log.warn("停止连接器失败: {}", e.getMessage());
            }
        }

        config.setRunningStatus(RunningStatus.STOP);
        cdcConfigRepository.update(config);
    }

    /**
     * 创建 Debezium 连接器
     */
    private void createDebeziumConnector(CdcConfig config, DsConfig dsConfig, DatasourceInfo info) {
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

        if (enabledConfigs.isEmpty()) {
            // 没有启用的表，设置空列表
            updateConnectorConfig(connectorName, info, dsConfig, "");
            return;
        }

        String tableIncludeList = enabledConfigs.stream()
                .map(c -> c.getDbName() + "." + c.getTableName())
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));

        updateConnectorConfig(connectorName, info, dsConfig, tableIncludeList);
    }

    /**
     * 更新单个连接器配置
     */
    private void updateConnectorConfig(String connectorName, DatasourceInfo info,
                                       DsConfig dsConfig, String tableIncludeList) {
        String historyTopic = "schema-history-" + info.hostname() + "-" + info.port();
        MySQLConnectorConfig mysqlConfig = new MySQLConnectorConfig()
                .setTopicPrefix(connectorName)
                .setTaskMax("1")
                .setHostname(info.hostname())
                .setPort(info.port())
                .setUser(dsConfig.getUsername())
                .setPassword(dsConfig.getPassword())
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

    private DatasourceInfo parseJdbcUrl(String jdbcUrl) {
        Matcher matcher = JDBC_URL_PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new SilentException("无法解析 JDBC URL: " + jdbcUrl);
        }
        return new DatasourceInfo(matcher.group(1), matcher.group(2));
    }

    private record DatasourceInfo(String hostname, String port) {
    }
}
