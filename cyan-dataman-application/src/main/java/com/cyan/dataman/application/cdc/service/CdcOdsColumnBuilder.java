package com.cyan.dataman.application.cdc.service;

import com.cyan.dataman.enums.SecretLevel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * CDC ODS 表字段构建器
 *
 * @author cy.Y
 * @since 1.0.0
 */
public final class CdcOdsColumnBuilder {

    /**
     * CDC 元字段名称
     */
    private static final List<String> CDC_METADATA_COLUMN_NAMES =
            List.of("_op", "_ts", "_db", "_table", "_ingestion_time");

    private CdcOdsColumnBuilder() {
    }

    /**
     * 构建 Flink CDC ODS 表字段
     */
    public static List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> buildFlinkOdsColumns(
            List<com.cyan.dataman.domain.ds.valobj.ColumnValObj> sourceColumns,
            SecretLevel secretLevel) {
        SecretLevel actualSecretLevel = secretLevel == null ? SecretLevel.L1 : secretLevel;
        List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> columns = new ArrayList<>();
        Set<String> sourceColumnNames = new LinkedHashSet<>();

        for (com.cyan.dataman.domain.ds.valobj.ColumnValObj sourceColumn : Optional.ofNullable(sourceColumns).orElse(List.of())) {
            if (sourceColumn == null || sourceColumn.getName() == null) {
                continue;
            }
            sourceColumnNames.add(sourceColumn.getName());
            columns.add(toMetadataColumn(sourceColumn, actualSecretLevel));
        }

        appendCdcMetadataColumns(columns, sourceColumnNames, actualSecretLevel);
        return deduplicate(columns);
    }

    /**
     * 判断是否为 CDC 元字段
     */
    public static boolean isCdcMetadataColumn(String columnName) {
        return CDC_METADATA_COLUMN_NAMES.contains(columnName);
    }

    /**
     * 转换源表字段
     */
    private static com.cyan.dataman.domain.metadata.valobj.ColumnValObj toMetadataColumn(
            com.cyan.dataman.domain.ds.valobj.ColumnValObj sourceColumn,
            SecretLevel secretLevel) {
        return new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                .setName(sourceColumn.getName())
                .setType(sourceColumn.getType())
                .setComment(sourceColumn.getComment())
                .setNullable(true)
                .setAutoIncrement(sourceColumn.getAutoIncrement())
                .setDefaultValue(sourceColumn.getDefaultValue())
                .setPrecision(sourceColumn.getPrecision())
                .setScale(sourceColumn.getScale())
                .setSecretLevel(secretLevel);
    }

    /**
     * 补充 CDC 元字段
     */
    private static void appendCdcMetadataColumns(List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> columns,
                                                Set<String> sourceColumnNames,
                                                SecretLevel secretLevel) {
        if (!sourceColumnNames.contains("_op")) {
            columns.add(cdcColumn("_op", "STRING", "操作类型", secretLevel));
        }
        if (!sourceColumnNames.contains("_ts")) {
            columns.add(cdcColumn("_ts", "LONG", "变更时间戳", secretLevel));
        }
        if (!sourceColumnNames.contains("_db")) {
            columns.add(cdcColumn("_db", "STRING", "源数据库", secretLevel));
        }
        if (!sourceColumnNames.contains("_table")) {
            columns.add(cdcColumn("_table", "STRING", "源表名", secretLevel));
        }
        if (!sourceColumnNames.contains("_ingestion_time")) {
            columns.add(cdcColumn("_ingestion_time", "TIMESTAMP_TZ", "入库时间", secretLevel));
        }
    }

    /**
     * 构建 CDC 元字段
     */
    private static com.cyan.dataman.domain.metadata.valobj.ColumnValObj cdcColumn(String name,
                                                                                 String type,
                                                                                 String comment,
                                                                                 SecretLevel secretLevel) {
        return new com.cyan.dataman.domain.metadata.valobj.ColumnValObj()
                .setName(name)
                .setType(type)
                .setComment(comment)
                .setNullable(true)
                .setAutoIncrement(false)
                .setSecretLevel(secretLevel);
    }

    /**
     * 按字段名去重
     */
    private static List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> deduplicate(
            List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> columns) {
        List<com.cyan.dataman.domain.metadata.valobj.ColumnValObj> uniqueColumns = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (com.cyan.dataman.domain.metadata.valobj.ColumnValObj column : columns) {
            if (column != null && seen.add(column.getName())) {
                uniqueColumns.add(column);
            }
        }
        return uniqueColumns;
    }
}
