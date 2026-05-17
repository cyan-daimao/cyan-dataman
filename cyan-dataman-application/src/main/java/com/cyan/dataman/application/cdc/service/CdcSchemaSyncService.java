package com.cyan.dataman.application.cdc.service;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.ds.valobj.ColumnValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.enums.SyncTool;
import com.cyan.dataman.infra.util.IcebergUtil;
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
    private final IcebergUtil icebergUtil;
    private final CdcFlinkSyncService cdcFlinkSyncService;

    public CdcSchemaSyncService(CdcConfigRepository cdcConfigRepository,
                                MetadataTableRepository metadataTableRepository,
                                IcebergUtil icebergUtil,
                                CdcFlinkSyncService cdcFlinkSyncService) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.metadataTableRepository = metadataTableRepository;
        this.icebergUtil = icebergUtil;
        this.cdcFlinkSyncService = cdcFlinkSyncService;
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
        CdcConfig config = configs.get(0);

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

        // 5. 更新 Iceberg 表结构（通过 Gravitino）
        boolean icebergOk = icebergUtil.addColumns("ods", odsTableName, newColumns);
        if (!icebergOk) {
            log.error("更新 Iceberg 表结构失败: ods.{}", odsTableName);
            throw new SilentException("更新 Iceberg 表结构失败: ods." + odsTableName);
        }

        // 6. 更新元数据平台记录
        List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> allColumns = new ArrayList<>(existingColumns);
        allColumns.addAll(newColumns);
        if (metadataTable.getTable() != null) {
            metadataTable.getTable().setColumns(allColumns);
        }
        metadataTableRepository.updateById(metadataTable);
        log.info("元数据平台记录已更新: {}，新增 {} 个字段", odsTableName, newColumns.size());

        // 7. 重启 Flink 作业（重新生成 SQL 并提交）
        cdcFlinkSyncService.restartFlinkJob(dsName, config.getSubjectCode());

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
}
