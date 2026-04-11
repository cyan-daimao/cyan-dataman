package com.cyan.dataman.infra.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.gravitino.Catalog;
import org.apache.gravitino.NameIdentifier;
import org.apache.gravitino.client.GravitinoClient;
import org.apache.gravitino.rel.Table;
import org.apache.gravitino.rel.TableCatalog;
import org.apache.gravitino.rel.TableChange;
import org.apache.gravitino.rel.types.Types;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * iceberg工具类
 * @author cy.Y
 * @since v1.0.0
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
}
