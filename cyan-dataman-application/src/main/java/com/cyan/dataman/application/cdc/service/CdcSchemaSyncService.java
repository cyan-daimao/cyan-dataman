package com.cyan.dataman.application.cdc.service;

import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.ds.valobj.ColumnValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.domain.metadata.valobj.TableValObj;
import com.cyan.dataman.enums.DataLayer;
import com.cyan.dataman.enums.SyncTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CDC Schema 同步服务
 * <p>
 * 当业务库表结构发生变更时，自动同步到对应的 Iceberg ODS 表，
 * 并重启 Flink 作业以应用新的 Schema。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class CdcSchemaSyncService {

    private final CdcConfigRepository cdcConfigRepository;
    private final MetadataTableRepository metadataTableRepository;
    private final MetadataTableService metadataTableService;
    private final CdcFlinkSyncService cdcFlinkSyncService;
    private final CdcFieldLineageSyncService cdcFieldLineageSyncService;

    public CdcSchemaSyncService(CdcConfigRepository cdcConfigRepository,
                                MetadataTableRepository metadataTableRepository,
                                MetadataTableService metadataTableService,
                                CdcFlinkSyncService cdcFlinkSyncService,
                                CdcFieldLineageSyncService cdcFieldLineageSyncService) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.metadataTableRepository = metadataTableRepository;
        this.metadataTableService = metadataTableService;
        this.cdcFlinkSyncService = cdcFlinkSyncService;
        this.cdcFieldLineageSyncService = cdcFieldLineageSyncService;
    }

    /**
     * 同步源表 Schema 变更到 CDC ODS 表
     *
     * @param dsName    数据源名称
     * @param dbName    数据库名
     * @param tableName 表名
     * @param newSchema 新的表结构
     */
    public void syncSchema(String dsName, String dbName, String tableName, TableSchemaValObj newSchema) {
        // 1. 查找对应的 CDC 配置（仅处理 Flink 类型且已启用的）
        CdcConfigListQuery query = new CdcConfigListQuery()
                .setDsName(dsName)
                .setDbName(dbName)
                .setTableName(tableName)
                .setSyncTool(SyncTool.FLINK)
                .setEnabled(true);
        List<CdcConfig> configs = cdcConfigRepository.list(query);
        if (configs == null || configs.isEmpty()) {
            log.info("未找到 CDC 配置，跳过 Schema 同步: {}.{}.{}", dsName, dbName, tableName);
            return;
        }
        CdcConfig config = configs.getFirst();

        // 2. 获取 ODS 表名
        String odsTableName = config.getIcebergTableName();
        if (odsTableName == null) {
            log.warn("CDC 配置缺少 Iceberg 表名: {}", config.getName());
            return;
        }

        // 3. 查找元数据平台中的 ODS 表
        MetadataTable metadataTable = metadataTableRepository.findOne(new MetadataTableOneQuery().setName(odsTableName));
        if (metadataTable == null) {
            log.warn("元数据平台中未找到 ODS 表: {}", odsTableName);
            return;
        }

        // 加载完整表信息（包含字段）
        metadataTable = metadataTableRepository.findById(metadataTable.getId());
        List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> existingColumns =
                metadataTable.getTable() != null ? metadataTable.getTable().getColumns() : List.of();
        Set<String> existingNames = existingColumns.stream()
                .map(com.cyan.dataman.domain.metadata.valobj.ColumnValObj::getName)
                .collect(Collectors.toSet());

        if (newSchema.getColumns() == null || newSchema.getColumns().isEmpty()) {
            log.info("新 Schema 字段为空，跳过 Schema 同步: {}.{}.{}", dsName, dbName, tableName);
            return;
        }

        // 4. 对比找出新增字段（排除元数据字段）
        List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> newColumns = new ArrayList<>();
        for (ColumnValObj sourceCol : newSchema.getColumns()) {
            String colName = sourceCol.getName();
            if (!existingNames.contains(colName) && !isMetadataColumn(colName)) {
                com.cyan.dataman.domain.metadata.valobj.ColumnValObj col =
                        new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                                .setName(colName)
                                .setType(sourceCol.getType())
                                .setComment(sourceCol.getComment())
                                .setNullable(true)
                                .setPrecision(sourceCol.getPrecision())
                                .setScale(sourceCol.getScale());
                newColumns.add(col);
            }
        }

        if (newColumns.isEmpty()) {
            log.info("没有新增字段，跳过 Schema 同步: {}.{}.{}", dsName, dbName, tableName);
            return;
        }

        log.info("检测到 {} 个新增字段，开始同步 Schema: {}.{}.{}", newColumns.size(), dsName, dbName, tableName);

        // 5. 通过元数据平台服务统一更新元数据记录与数仓表结构
        TableValObj tableValObj = metadataTable.getTable();
        if (tableValObj == null) {
            log.warn("元数据平台中的 ODS 表缺少表结构，跳过 Schema 同步: {}", odsTableName);
            return;
        }
        List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> allColumns = new ArrayList<>(existingColumns);
        allColumns.addAll(newColumns);
        tableValObj.setColumns(allColumns);

        MetadataTableCmd cmd = new MetadataTableCmd()
                .setName(metadataTable.getName())
                .setOwner(metadataTable.getOwner())
                .setSubjectCode(metadataTable.getSubjectCode())
                .setLayerCode(toDataLayer(metadataTable.getLayerCode()))
                .setComment(metadataTable.getComment())
                .setHeatLevel(metadataTable.getHeatLevel())
                .setSecretLevel(metadataTable.getSecretLevel())
                .setOnlineStatus(metadataTable.getOnlineStatus())
                .setTableValObj(tableValObj);
        metadataTableService.update(metadataTable.getId(), cmd);
        log.info("元数据平台记录已更新: {}，新增 {} 个字段", odsTableName, newColumns.size());

        // 6. 重启 Flink 作业（重新生成 SQL 并提交）
        cdcFlinkSyncService.restartFlinkJob(config.getId());
        cdcFieldLineageSyncService.syncFlinkConfig(config);

        log.info("Schema 同步完成，新增 {} 个字段，Flink 作业已重启: {}.{}.{}",
                newColumns.size(), dsName, dbName, tableName);
    }

    /**
     * 判断是否为 CDC 元数据字段
     */
    private boolean isMetadataColumn(String name) {
        return "_op".equals(name) || "_ts".equals(name) || "_db".equals(name)
                || "_table".equals(name) || "_ingestion_time".equals(name);
    }

    /**
     * 转换数据层级
     */
    private DataLayer toDataLayer(String layerCode) {
        DataLayer dataLayer = DataLayer.getByCode(layerCode);
        return dataLayer != null ? dataLayer : DataLayer.ODS;
    }
}
