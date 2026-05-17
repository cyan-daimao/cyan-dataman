package com.cyan.dataman.infra.util;

import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.enums.ColumnDataType;
import lombok.extern.slf4j.Slf4j;
import org.apache.gravitino.Catalog;
import org.apache.gravitino.NameIdentifier;
import org.apache.gravitino.client.GravitinoClient;
import org.apache.gravitino.rel.Table;
import org.apache.gravitino.rel.TableCatalog;
import org.apache.gravitino.rel.TableChange;
import org.apache.gravitino.rel.types.Type;
import org.apache.gravitino.rel.types.Types;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Iceberg 工具类
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class IcebergUtil {

    private static final String OP_COLUMN_NAME = "op";
    private static final String OP_COLUMN_COMMENT = "CDC operation type: c(create), r(read), u(update), d(delete)";

    private final GravitinoClient gravitinoClient;

    public IcebergUtil(GravitinoClient gravitinoClient) {
        this.gravitinoClient = gravitinoClient;
    }

    /**
     * 检查并确保 op 字段存在，如果不存在则创建
     * @param schema 库名
     * @param tableName 表名
     * @return true 表示已存在或创建成功，false 表示表不存在或其他异常
     */
    public boolean ensureOpColumnExists(String schema, String tableName) {
        try {
            Catalog catalog = gravitinoClient.loadCatalog("iceberg");
            TableCatalog tableCatalog = catalog.asTableCatalog();
            NameIdentifier tableIdent = NameIdentifier.of(schema, tableName);

            // 检查表是否存在
            if (!tableCatalog.tableExists(tableIdent)) {
                log.warn("Iceberg 表不存在: {}.{}", schema, tableName);
                return false;
            }

            // 加载表并检查 op 字段是否存在
            Table table = tableCatalog.loadTable(tableIdent);
            boolean opColumnExists = Arrays.stream(table.columns())
                    .anyMatch(col -> OP_COLUMN_NAME.equals(col.name()));

            if (opColumnExists) {
                log.info("Iceberg 表 {}.{} 已存在 op 字段", schema, tableName);
                return true;
            }

            // 添加 op 字段
            TableChange addOpColumn = TableChange.addColumn(
                    new String[]{OP_COLUMN_NAME},
                    Types.StringType.get(),
                    OP_COLUMN_COMMENT,
                    true
            );
            tableCatalog.alterTable(tableIdent, addOpColumn);
            log.info("成功为 Iceberg 表 {}.{} 添加 op 字段", schema, tableName);
            return true;
        } catch (Exception e) {
            log.error("检查/创建 op 字段失败: {}.{}, 错误: {}", schema, tableName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查指定字段是否存在
     * @param schema 库名
     * @param tableName 表名
     * @param columnName 字段名
     * @return true 表示字段存在
     */
    public boolean columnExists(String schema, String tableName, String columnName) {
        try {
            Catalog catalog = gravitinoClient.loadCatalog("iceberg");
            TableCatalog tableCatalog = catalog.asTableCatalog();
            NameIdentifier tableIdent = NameIdentifier.of(schema, tableName);

            if (!tableCatalog.tableExists(tableIdent)) {
                return false;
            }

            Table table = tableCatalog.loadTable(tableIdent);
            return Arrays.stream(table.columns())
                    .anyMatch(col -> columnName.equals(col.name()));
        } catch (Exception e) {
            log.error("检查字段存在失败: {}.{}, 字段: {}, 错误: {}", schema, tableName, columnName, e.getMessage());
            return false;
        }
    }

    /**
     * 批量添加字段到 Iceberg 表（已存在的字段自动跳过）
     *
     * @param schema    库名
     * @param tableName 表名
     * @param columns   待添加的字段列表
     * @return true 表示成功（或没有需要添加的字段）
     */
    public boolean addColumns(String schema, String tableName, List<ColumnValObj> columns) {
        if (columns == null || columns.isEmpty()) {
            return true;
        }
        try {
            Catalog catalog = gravitinoClient.loadCatalog("iceberg");
            TableCatalog tableCatalog = catalog.asTableCatalog();
            NameIdentifier tableIdent = NameIdentifier.of(schema, tableName);

            if (!tableCatalog.tableExists(tableIdent)) {
                log.warn("Iceberg 表不存在，无法添加字段: {}.{}", schema, tableName);
                return false;
            }

            Table table = tableCatalog.loadTable(tableIdent);
            Set<String> existingNames = Arrays.stream(table.columns())
                    .map(org.apache.gravitino.rel.Column::name)
                    .collect(Collectors.toSet());

            List<TableChange> changes = new ArrayList<>();
            for (ColumnValObj col : columns) {
                if (existingNames.contains(col.getName())) {
                    continue;
                }
                Type gravitinoType = toGravitinoType(col.getColumnDataType(), col.getPrecision(), col.getScale());
                changes.add(TableChange.addColumn(
                        new String[]{col.getName()},
                        gravitinoType,
                        col.getComment(),
                        true
                ));
            }

            if (!changes.isEmpty()) {
                tableCatalog.alterTable(tableIdent, changes.toArray(new TableChange[0]));
                log.info("成功为 Iceberg 表 {}.{} 添加 {} 个字段", schema, tableName, changes.size());
            }
            return true;
        } catch (Exception e) {
            log.error("添加字段失败: {}.{}, 错误: {}", schema, tableName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取 Iceberg 表已有字段名集合
     *
     * @param schema    库名
     * @param tableName 表名
     * @return 字段名集合，表不存在时返回空集合
     */
    public Set<String> listColumnNames(String schema, String tableName) {
        try {
            Catalog catalog = gravitinoClient.loadCatalog("iceberg");
            TableCatalog tableCatalog = catalog.asTableCatalog();
            NameIdentifier tableIdent = NameIdentifier.of(schema, tableName);

            if (!tableCatalog.tableExists(tableIdent)) {
                return Set.of();
            }

            Table table = tableCatalog.loadTable(tableIdent);
            return Arrays.stream(table.columns())
                    .map(org.apache.gravitino.rel.Column::name)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("获取字段列表失败: {}.{}, 错误: {}", schema, tableName, e.getMessage());
            return Set.of();
        }
    }

    /**
     * 将 ColumnDataType 转换为 Gravitino Type
     */
    private Type toGravitinoType(ColumnDataType dataType, Integer precision, Integer scale) {
        if (dataType == null) {
            return Types.StringType.get();
        }
        return switch (dataType) {
            case BOOLEAN -> Types.BooleanType.get();
            case INTEGER -> Types.IntegerType.get();
            case LONG -> Types.LongType.get();
            case FLOAT -> Types.FloatType.get();
            case DOUBLE -> Types.DoubleType.get();
            case DECIMAL -> {
                int p = precision != null && precision > 0 ? precision : 38;
                int s = scale != null && scale >= 0 ? scale : 0;
                yield Types.DecimalType.of(p, s);
            }
            case STRING -> Types.StringType.get();
            case DATE -> Types.DateType.get();
            case TIMESTAMP -> Types.TimestampType.withoutTimeZone();
            case TIMESTAMP_TZ -> Types.TimestampType.withTimeZone();
            case TIME -> Types.TimeType.get();
            case BINARY -> Types.BinaryType.get();
            case UUID -> Types.UUIDType.get();
        };
    }
}
