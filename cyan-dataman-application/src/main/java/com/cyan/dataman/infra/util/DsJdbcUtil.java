package com.cyan.dataman.infra.util;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.valobj.DatabaseValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.IndexValObj;
import com.cyan.dataman.enums.ColumnDataType;
import com.cyan.dataman.enums.DatasourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据源 JDBC 工具类
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class DsJdbcUtil {

    /**
     * 测试数据源连接
     */
    public void testConnection(DsConfig dsConfig) {
        try (Connection ignored = getConnection(dsConfig)) {
            // 连接成功
        } catch (SQLException e) {
            throw new SilentException("数据源连接失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection(DsConfig dsConfig) throws SQLException {
        return DriverManager.getConnection(dsConfig.getUrl(), dsConfig.getUsername(), dsConfig.getPassword());
    }

    /**
     * 获取数据库列表
     */
    public List<DatabaseValObj> listDatabases(DsConfig dsConfig) {
        List<DatabaseValObj> databases = new ArrayList<>();
        try (Connection conn = getConnection(dsConfig)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getCatalogs();
            while (rs.next()) {
                String dbName = rs.getString("TABLE_CAT");
                databases.add(new DatabaseValObj()
                        .setName(dbName)
                        .setComment(dbName));
            }
        } catch (SQLException e) {
            throw new SilentException("获取数据库列表失败: " + e.getMessage());
        }
        return databases;
    }

    /**
     * 创建数据库
     */
    public void createDatabase(DsConfig dsConfig, String dbName, String charset, String collation) {
        StringBuilder sql = new StringBuilder("CREATE DATABASE `").append(dbName).append("`");
        if (charset != null && !charset.isEmpty()) {
            sql.append(" CHARACTER SET ").append(charset);
        }
        if (collation != null && !collation.isEmpty()) {
            sql.append(" COLLATE ").append(collation);
        }
        executeUpdate(dsConfig, sql.toString());
    }

    /**
     * 获取表列表
     */
    public List<String> listTables(DsConfig dsConfig, String dbName) {
        List<String> tables = new ArrayList<>();
        String url = buildUrlWithDb(dsConfig, dbName);
        try (Connection conn = DriverManager.getConnection(url, dsConfig.getUsername(), dsConfig.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            throw new SilentException("获取表列表失败: " + e.getMessage());
        }
        return tables;
    }

    /**
     * 获取表结构详情
     */
    public TableSchemaValObj getTableSchema(DsConfig dsConfig, String dbName, String tableName) {
        String url = buildUrlWithDb(dsConfig, dbName);
        try (Connection conn = DriverManager.getConnection(url, dsConfig.getUsername(), dsConfig.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();

            // 获取表注释
            String tableComment = "";
            ResultSet tableRs = metaData.getTables(dbName, null, tableName, new String[]{"TABLE"});
            if (tableRs.next()) {
                tableComment = tableRs.getString("REMARKS");
            }

            // 获取字段信息
            List<ColumnValObj> columns = new ArrayList<>();
            ResultSet columnRs = metaData.getColumns(dbName, null, tableName, null);
            while (columnRs.next()) {
                ColumnValObj column = new ColumnValObj()
                        .setName(columnRs.getString("COLUMN_NAME"))
                        .setType(convertToColumnDataType(columnRs.getString("TYPE_NAME")))
                        .setComment(columnRs.getString("REMARKS"))
                        .setNullable(columnRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                        .setDefaultValue(columnRs.getString("COLUMN_DEF"));
                columns.add(column);
            }

            // 获取索引信息
            List<IndexValObj> indexes = new ArrayList<>();
            ResultSet indexRs = metaData.getIndexInfo(dbName, null, tableName, false, false);
            String lastIndexName = null;
            List<String> lastIndexColumns = new ArrayList<>();
            while (indexRs.next()) {
                String indexName = indexRs.getString("INDEX_NAME");
                if (indexName == null) {
                    continue;
                }
                String columnName = indexRs.getString("COLUMN_NAME");
                boolean nonUnique = indexRs.getBoolean("NON_UNIQUE");

                if (lastIndexName != null && !lastIndexName.equals(indexName)) {
                    if (!lastIndexColumns.isEmpty()) {
                        indexes.add(new IndexValObj()
                                .setName(lastIndexName)
                                .setIndexType(nonUnique ? "INDEX" : "UNIQUE")
                                .setFieldNames(new ArrayList<>(lastIndexColumns)));
                    }
                    lastIndexColumns.clear();
                }
                lastIndexName = indexName;
                lastIndexColumns.add(columnName);
            }
            // 添加最后一个索引
            if (lastIndexName != null && !lastIndexColumns.isEmpty()) {
                indexes.add(new IndexValObj()
                        .setName(lastIndexName)
                        .setIndexType("INDEX")
                        .setFieldNames(new ArrayList<>(lastIndexColumns)));
            }

            return new TableSchemaValObj()
                    .setTableName(tableName)
                    .setTableComment(tableComment)
                    .setColumns(columns)
                    .setIndexes(indexes);
        } catch (SQLException e) {
            throw new SilentException("获取表结构失败: " + e.getMessage());
        }
    }

    /**
     * 创建表
     */
    public void createTable(DsConfig dsConfig, String dbName, TableSchemaValObj tableSchema) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(tableSchema.getTableName()).append("` (\n");

        // 添加字段
        List<String> columnDefs = new ArrayList<>();
        for (ColumnValObj column : tableSchema.getColumns()) {
            columnDefs.add(buildColumnDefinition(column));
        }

        // 添加主键（如果第一个索引是 PRIMARY）
        boolean hasPrimaryKey = tableSchema.getIndexes() != null && 
                tableSchema.getIndexes().stream().anyMatch(i -> "PRIMARY".equalsIgnoreCase(i.getName()));
        if (!hasPrimaryKey) {
            // 默认主键为 id
            columnDefs.add("PRIMARY KEY (`id`)");
        }

        sql.append(String.join(",\n", columnDefs));
        sql.append("\n)");

        if (tableSchema.getTableComment() != null && !tableSchema.getTableComment().isEmpty()) {
            sql.append(" COMMENT '").append(escapeString(tableSchema.getTableComment())).append("'");
        }

        String url = buildUrlWithDb(dsConfig, dbName);
        executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), sql.toString());

        // 创建索引
        if (tableSchema.getIndexes() != null) {
            for (IndexValObj index : tableSchema.getIndexes()) {
                if (!"PRIMARY".equalsIgnoreCase(index.getName())) {
                    createIndex(dsConfig, dbName, tableSchema.getTableName(), index);
                }
            }
        }
    }

    /**
     * 修改表结构
     */
    public void updateTable(DsConfig dsConfig, String dbName, String oldTableName, TableSchemaValObj tableSchema) {
        String url = buildUrlWithDb(dsConfig, dbName);
        
        // 如果表名变更，先重命名
        if (!oldTableName.equals(tableSchema.getTableName())) {
            String renameSql = String.format("RENAME TABLE `%s` TO `%s`", oldTableName, tableSchema.getTableName());
            executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), renameSql);
        }

        // 获取旧表结构
        TableSchemaValObj oldSchema = getTableSchema(dsConfig, dbName, tableSchema.getTableName());

        // 修改表注释
        if (tableSchema.getTableComment() != null && !tableSchema.getTableComment().equals(oldSchema.getTableComment())) {
            String commentSql = String.format("ALTER TABLE `%s` COMMENT '%s'", 
                    tableSchema.getTableName(), escapeString(tableSchema.getTableComment()));
            executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), commentSql);
        }

        // 修改字段（简化处理：删除旧字段，添加新字段）
        // 实际场景需要更复杂的 diff 逻辑
        // 这里只做简单的示例

        // 修改索引
        // 先删除旧索引，再添加新索引
        if (tableSchema.getIndexes() != null && oldSchema.getIndexes() != null) {
            for (IndexValObj oldIndex : oldSchema.getIndexes()) {
                if (!"PRIMARY".equalsIgnoreCase(oldIndex.getName())) {
                    dropIndex(dsConfig, dbName, tableSchema.getTableName(), oldIndex.getName());
                }
            }
            for (IndexValObj newIndex : tableSchema.getIndexes()) {
                if (!"PRIMARY".equalsIgnoreCase(newIndex.getName())) {
                    createIndex(dsConfig, dbName, tableSchema.getTableName(), newIndex);
                }
            }
        }
    }

    /**
     * 删除表
     */
    public void dropTable(DsConfig dsConfig, String dbName, String tableName) {
        String sql = String.format("DROP TABLE IF EXISTS `%s`", tableName);
        String url = buildUrlWithDb(dsConfig, dbName);
        executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), sql);
    }

    /**
     * 创建索引
     */
    private void createIndex(DsConfig dsConfig, String dbName, String tableName, IndexValObj index) {
        String url = buildUrlWithDb(dsConfig, dbName);
        String columns = index.getFieldNames().stream()
                .map(col -> "`" + col + "`")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        
        String indexType = switch (index.getIndexType().toUpperCase()) {
            case "UNIQUE" -> "UNIQUE INDEX";
            case "FULLTEXT" -> "FULLTEXT INDEX";
            default -> "INDEX";
        };

        String sql = String.format("ALTER TABLE `%s` ADD %s `%s` (%s)", 
                tableName, indexType, index.getName(), columns);
        executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), sql);
    }

    /**
     * 删除索引
     */
    private void dropIndex(DsConfig dsConfig, String dbName, String tableName, String indexName) {
        String url = buildUrlWithDb(dsConfig, dbName);
        String sql = String.format("ALTER TABLE `%s` DROP INDEX `%s`", tableName, indexName);
        executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), sql);
    }

    /**
     * 构建字段定义
     */
    private String buildColumnDefinition(ColumnValObj column) {
        StringBuilder def = new StringBuilder();
        def.append("  `").append(column.getName()).append("` ");
        def.append(convertToMysqlType(column.getType()));
        
        if (column.getPrecision() != null && column.getScale() != null) {
            def.append("(").append(column.getPrecision()).append(",").append(column.getScale()).append(")");
        } else if (column.getPrecision() != null) {
            def.append("(").append(column.getPrecision()).append(")");
        }

        if (Boolean.TRUE.equals(column.getNullable())) {
            def.append(" NULL");
        } else {
            def.append(" NOT NULL");
        }

        if (column.getDefaultValue() != null) {
            def.append(" DEFAULT '").append(escapeString(column.getDefaultValue())).append("'");
        }

        if (Boolean.TRUE.equals(column.getAutoIncrement())) {
            def.append(" AUTO_INCREMENT");
        }

        if (column.getComment() != null && !column.getComment().isEmpty()) {
            def.append(" COMMENT '").append(escapeString(column.getComment())).append("'");
        }

        return def.toString();
    }

    /**
     * 转换为 MySQL 类型
     */
    private String convertToMysqlType(ColumnDataType type) {
        return switch (type) {
            case BOOLEAN -> "TINYINT(1)";
            case INTEGER -> "INT";
            case LONG -> "BIGINT";
            case FLOAT -> "FLOAT";
            case DOUBLE -> "DOUBLE";
            case DECIMAL -> "DECIMAL";
            case STRING -> "VARCHAR(255)";
            case DATE -> "DATE";
            case TIMESTAMP, TIMESTAMP_TZ -> "DATETIME";
            case TIME -> "TIME";
            case BINARY -> "BLOB";
            case UUID -> "VARCHAR(36)";
        };
    }

    /**
     * 从数据库类型转换为 ColumnDataType
     */
    private ColumnDataType convertToColumnDataType(String mysqlType) {
        if (mysqlType == null) {
            return ColumnDataType.STRING;
        }
        String upperType = mysqlType.toUpperCase();
        if (upperType.contains("INT") || upperType.contains("TINYINT")) {
            return upperType.contains("BIGINT") ? ColumnDataType.LONG : ColumnDataType.INTEGER;
        }
        if (upperType.contains("FLOAT")) {
            return ColumnDataType.FLOAT;
        }
        if (upperType.contains("DOUBLE") || upperType.contains("DECIMAL")) {
            return ColumnDataType.DOUBLE;
        }
        if (upperType.contains("DATE")) {
            return ColumnDataType.DATE;
        }
        if (upperType.contains("TIME")) {
            return ColumnDataType.TIMESTAMP;
        }
        if (upperType.contains("BLOB") || upperType.contains("BINARY")) {
            return ColumnDataType.BINARY;
        }
        if (upperType.contains("BOOL")) {
            return ColumnDataType.BOOLEAN;
        }
        return ColumnDataType.STRING;
    }

    /**
     * 构建带数据库的 URL
     */
    private String buildUrlWithDb(DsConfig dsConfig, String dbName) {
        if (dsConfig.getDatasourceType() == DatasourceType.MYSQL) {
            String url = dsConfig.getUrl();
            if (url.contains("?")) {
                return url.replace("?", "/" + dbName + "?");
            } else {
                return url.endsWith("/") ? url + dbName : url + "/" + dbName;
            }
        }
        return dsConfig.getUrl();
    }

    /**
     * 执行更新语句
     */
    private void executeUpdate(DsConfig dsConfig, String sql) {
        try (Connection conn = getConnection(dsConfig);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new SilentException("执行SQL失败: " + e.getMessage() + ", SQL: " + sql);
        }
    }

    /**
     * 执行更新语句
     */
    private void executeUpdate(String url, String username, String password, String sql) {
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new SilentException("执行SQL失败: " + e.getMessage() + ", SQL: " + sql);
        }
    }

    /**
     * 转义字符串
     */
    private String escapeString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''");
    }
}
