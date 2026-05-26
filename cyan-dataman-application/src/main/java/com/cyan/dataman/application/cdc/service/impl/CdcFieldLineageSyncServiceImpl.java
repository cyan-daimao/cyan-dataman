package com.cyan.dataman.application.cdc.service.impl;

import com.alibaba.fastjson2.JSON;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataman.application.cdc.service.CdcFieldLineageSyncService;
import com.cyan.dataman.application.ds.DsConfigService;
import com.cyan.dataman.application.metadata.lineage.MetadataLineageService;
import com.cyan.dataman.application.metadata.lineage.cmd.MetadataLineageSyncCmd;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.domain.ds.valobj.ColumnValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CDC 字段血缘同步服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class CdcFieldLineageSyncServiceImpl implements CdcFieldLineageSyncService {

    private static final String SERVICE_NAME = "cyan-dataman";
    private static final String NODE_FIELD = "FIELD";
    private static final String NODE_ETL_JOB = "ETL_JOB";
    private static final String EDGE_READS_FIELD = "READS_FIELD";
    private static final String EDGE_WRITES_FIELD = "WRITES_FIELD";
    private static final List<String> FLINK_CDC_SYSTEM_COLUMNS = List.of("_op", "_ts", "_db", "_table", "_ingestion_time");

    private final MetadataLineageService metadataLineageService;
    private final DsConfigService dsConfigService;

    public CdcFieldLineageSyncServiceImpl(MetadataLineageService metadataLineageService,
                                          DsConfigService dsConfigService) {
        this.metadataLineageService = metadataLineageService;
        this.dsConfigService = dsConfigService;
    }

    /**
     * 同步 Flink CDC 配置字段血缘
     */
    @Override
    public void syncFlinkConfig(CdcConfig config) {
        if (config == null || StrUtils.isBlank(config.getId())) {
            return;
        }
        String refId = flinkRefId(config.getId());
        try {
            FieldFetchResult fieldFetchResult = fetchSourceFieldNames(config);
            TargetTable targetTable = flinkTargetTable(config);
            String jobKey = "etl_job:dataman:cdc_flink:" + config.getId();
            List<String> targetColumns = StrUtils.isBlank(fieldFetchResult.errorMessage())
                    ? appendFlinkSystemColumns(fieldFetchResult.fieldNames())
                    : List.of();

            MetadataLineageSyncCmd cmd = buildSyncCmd(
                    refId,
                    jobKey,
                    "Flink CDC: " + sourceTableRef(config) + " -> " + targetTable.tableRef(),
                    config,
                    null,
                    targetTable,
                    fieldFetchResult.fieldNames(),
                    targetColumns,
                    fieldFetchResult.errorMessage());
            metadataLineageService.sync(cmd);
        } catch (Exception e) {
            log.warn("同步 Flink CDC 字段血缘失败，已跳过: cdcConfigId={}, error={}", config.getId(), e.getMessage(), e);
        }
    }

    /**
     * 清理 Flink CDC 配置字段血缘
     */
    @Override
    public void clearFlinkConfig(String cdcConfigId) {
        clearByRefId(flinkRefId(cdcConfigId));
    }

    /**
     * 同步 Spark CDC 作业字段血缘
     */
    @Override
    public void syncSparkJob(CdcSparkJob job, CdcConfig config) {
        if (job == null || config == null || StrUtils.isBlank(job.getId())) {
            return;
        }
        String refId = sparkRefId(job.getId());
        try {
            FieldFetchResult fieldFetchResult = fetchSourceFieldNames(config);
            TargetTable targetTable = sparkTargetTable(config);
            String jobKey = "etl_job:dataman:cdc_spark:" + job.getId();

            MetadataLineageSyncCmd cmd = buildSyncCmd(
                    refId,
                    jobKey,
                    "Spark CDC: " + sourceTableRef(config) + " -> " + targetTable.tableRef(),
                    config,
                    job,
                    targetTable,
                    fieldFetchResult.fieldNames(),
                    fieldFetchResult.fieldNames(),
                    fieldFetchResult.errorMessage());
            metadataLineageService.sync(cmd);
        } catch (Exception e) {
            log.warn("同步 Spark CDC 字段血缘失败，已跳过: sparkJobId={}, cdcConfigId={}, error={}",
                    job.getId(), config.getId(), e.getMessage(), e);
        }
    }

    /**
     * 清理 Spark CDC 作业字段血缘
     */
    @Override
    public void clearSparkJob(String sparkJobId) {
        clearByRefId(sparkRefId(sparkJobId));
    }

    /**
     * 构建血缘同步命令
     */
    private MetadataLineageSyncCmd buildSyncCmd(String refId,
                                                String jobKey,
                                                String jobName,
                                                CdcConfig config,
                                                CdcSparkJob sparkJob,
                                                TargetTable targetTable,
                                                List<String> sourceColumns,
                                                List<String> targetColumns,
                                                String columnFetchError) {
        List<MetadataLineageSyncCmd.LineageNodeCmd> nodes = new ArrayList<>();
        List<MetadataLineageSyncCmd.LineageEdgeCmd> edges = new ArrayList<>();

        nodes.add(new MetadataLineageSyncCmd.LineageNodeCmd()
                .setNodeKey(jobKey)
                .setNodeType(NODE_ETL_JOB)
                .setNodeName(jobName)
                .setPropertiesJson(jobProperties(config, sparkJob, targetTable, columnFetchError)));

        for (String column : sourceColumns) {
            String fieldKey = sourceFieldKey(config, column);
            nodes.add(fieldNode(fieldKey, sourceTableRef(config), column));
            edges.add(edge(fieldKey, jobKey, EDGE_READS_FIELD));
        }
        for (String column : targetColumns) {
            String fieldKey = targetFieldKey(targetTable, column);
            nodes.add(fieldNode(fieldKey, targetTable.tableRef(), column));
            edges.add(edge(jobKey, fieldKey, EDGE_WRITES_FIELD));
        }

        return new MetadataLineageSyncCmd()
                .setServiceName(SERVICE_NAME)
                .setRefId(refId)
                .setNodes(nodes)
                .setEdges(edges);
    }

    /**
     * 查询源表字段名
     */
    private FieldFetchResult fetchSourceFieldNames(CdcConfig config) {
        try {
            TableSchemaValObj schema = dsConfigService.getTableSchema(config.getDsName(), config.getDbName(), config.getTableName());
            List<String> fieldNames = schema == null || schema.getColumns() == null
                    ? List.of()
                    : schema.getColumns().stream()
                    .map(ColumnValObj::getName)
                    .filter(StrUtils::isNotBlank)
                    .distinct()
                    .toList();
            if (fieldNames.isEmpty()) {
                return new FieldFetchResult(List.of(), "源表字段为空");
            }
            return new FieldFetchResult(fieldNames, null);
        } catch (Exception e) {
            log.warn("获取 CDC 源表字段失败: {}.{}.{}, error={}",
                    config.getDsName(), config.getDbName(), config.getTableName(), e.getMessage());
            return new FieldFetchResult(List.of(), e.getMessage());
        }
    }

    /**
     * 追加 Flink CDC 系统字段
     */
    private List<String> appendFlinkSystemColumns(List<String> sourceColumns) {
        Set<String> columns = new LinkedHashSet<>(sourceColumns);
        columns.addAll(FLINK_CDC_SYSTEM_COLUMNS);
        return new ArrayList<>(columns);
    }

    /**
     * 清理指定来源血缘
     */
    private void clearByRefId(String refId) {
        if (StrUtils.isBlank(refId)) {
            return;
        }
        try {
            metadataLineageService.sync(new MetadataLineageSyncCmd()
                    .setServiceName(SERVICE_NAME)
                    .setRefId(refId)
                    .setNodes(List.of())
                    .setEdges(List.of()));
        } catch (Exception e) {
            log.warn("清理 CDC 字段血缘失败，已跳过: refId={}, error={}", refId, e.getMessage(), e);
        }
    }

    /**
     * 构建字段节点
     */
    private MetadataLineageSyncCmd.LineageNodeCmd fieldNode(String fieldKey, String tableRef, String column) {
        return new MetadataLineageSyncCmd.LineageNodeCmd()
                .setNodeKey(fieldKey)
                .setNodeType(NODE_FIELD)
                .setNodeName(tableRef + "." + column)
                .setTableRef(tableRef)
                .setColumnName(column);
    }

    /**
     * 构建血缘边
     */
    private MetadataLineageSyncCmd.LineageEdgeCmd edge(String sourceKey, String targetKey, String edgeType) {
        return new MetadataLineageSyncCmd.LineageEdgeCmd()
                .setSourceKey(sourceKey)
                .setTargetKey(targetKey)
                .setEdgeType(edgeType);
    }

    /**
     * 构建作业属性
     */
    private String jobProperties(CdcConfig config, CdcSparkJob sparkJob, TargetTable targetTable, String columnFetchError) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("syncTool", config.getSyncTool() == null ? null : config.getSyncTool().name());
        properties.put("cdcConfigId", config.getId());
        properties.put("cdcConfigName", config.getName());
        properties.put("sparkJobId", sparkJob == null ? null : sparkJob.getId());
        properties.put("syncMode", sparkJob == null || sparkJob.getSyncMode() == null ? null : sparkJob.getSyncMode().name());
        properties.put("enabled", sparkJob == null ? config.getEnabled() : sparkJob.getEnabled());
        properties.put("sourceTable", sourceTableRef(config));
        properties.put("targetTable", targetTable.tableRef());
        properties.put("columnFetchError", columnFetchError);
        return JSON.toJSONString(properties);
    }

    /**
     * 源字段 key
     */
    private String sourceFieldKey(CdcConfig config, String column) {
        return "field:" + sourceTableRef(config) + "." + column;
    }

    /**
     * 目标字段 key
     */
    private String targetFieldKey(TargetTable targetTable, String column) {
        return "field:" + targetTable.tableRef() + "." + column;
    }

    /**
     * 源表引用
     */
    private String sourceTableRef(CdcConfig config) {
        return config.getDsName() + "." + config.getDbName() + "." + config.getTableName();
    }

    /**
     * Flink 目标表
     */
    private TargetTable flinkTargetTable(CdcConfig config) {
        return new TargetTable("iceberg", "ods", config.getIcebergTableName());
    }

    /**
     * Spark 目标表
     */
    private TargetTable sparkTargetTable(CdcConfig config) {
        String icebergTableName = config.getIcebergTableName();
        if (StrUtils.isNotBlank(icebergTableName) && icebergTableName.contains(".")) {
            String[] parts = icebergTableName.split("\\.");
            return new TargetTable("iceberg", parts[0], parts[1]);
        }
        return new TargetTable("iceberg", "ods", icebergTableName);
    }

    /**
     * Flink 来源业务 ID
     */
    private String flinkRefId(String cdcConfigId) {
        return StrUtils.isBlank(cdcConfigId) ? null : "cdc_flink:" + cdcConfigId;
    }

    /**
     * Spark 来源业务 ID
     */
    private String sparkRefId(String sparkJobId) {
        return StrUtils.isBlank(sparkJobId) ? null : "cdc_spark:" + sparkJobId;
    }

    /**
     * 字段拉取结果
     */
    private record FieldFetchResult(List<String> fieldNames, String errorMessage) {
    }

    /**
     * 目标表信息
     */
    private record TargetTable(String catalog, String schema, String table) {

        /**
         * 表引用
         */
        private String tableRef() {
            return catalog + "." + schema + "." + table;
        }
    }
}
