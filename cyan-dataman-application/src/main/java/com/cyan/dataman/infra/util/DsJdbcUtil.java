package com.cyan.dataman.infra.util;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.valobj.ColumnValObj;
import com.cyan.dataman.domain.ds.valobj.IndexValObj;
import com.cyan.dataman.domain.ds.valobj.MysqlColumnValObj;
import com.cyan.dataman.domain.ds.valobj.PgsqlColumnValObj;
import com.cyan.dataman.domain.ds.valobj.DatabaseValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.enums.DatasourceType;
import com.cyan.dataman.enums.MysqlColumnType;
import com.cyan.dataman.enums.PostgresColumnType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        try {
            String driverClass = getDriverClass(dsConfig.getDatasourceType());
            Class.forName(driverClass);
            Connection ignored = getConnection(dsConfig);
        } catch (Exception e) {
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
        DatasourceType dsType = dsConfig.getDatasourceType();
        StringBuilder sql = new StringBuilder();
        
        if (dsType == DatasourceType.MYSQL) {
            sql.append("CREATE DATABASE `").append(dbName).append("`");
            if (charset != null && !charset.isEmpty()) {
                sql.append(" CHARACTER SET ").append(charset);
            }
            if (collation != null && !collation.isEmpty()) {
                sql.append(" COLLATE ").append(collation);
            }
        } else if (dsType == DatasourceType.POSTGRESQL) {
            sql.append("CREATE DATABASE \"").append(dbName).append("\"");
            if (charset != null && !charset.isEmpty()) {
                sql.append(" ENCODING '").append(charset).append("'");
            }
        } else {
            throw new SilentException("不支持的数据源类型: " + dsType);
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
                String dbTypeName = columnRs.getString("TYPE_NAME");
                int columnSize = columnRs.getInt("COLUMN_SIZE");
                String fullType = buildFullTypeName(dbTypeName, columnSize);
                
                ColumnValObj column = createColumnValObj(dsConfig.getDatasourceType())
                        .setName(columnRs.getString("COLUMN_NAME"))
                        .setType(fullType)
                        .setComment(columnRs.getString("REMARKS"))
                        .setNullable(columnRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                        .setDefaultValue(columnRs.getString("COLUMN_DEF"));
                columns.add(column);
            }

            // 获取索引信息
            List<IndexValObj> indexes = new ArrayList<>();
            ResultSet indexRs = metaData.getIndexInfo(dbName, null, tableName, false, false);
            String lastIndexName = null;
            boolean lastNonUnique = true;
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
                                .setIndexType(lastNonUnique ? "INDEX" : "UNIQUE")
                                .setFieldNames(new ArrayList<>(lastIndexColumns)));
                    }
                    lastIndexColumns.clear();
                }
                lastIndexName = indexName;
                lastNonUnique = nonUnique;
                lastIndexColumns.add(columnName);
            }
            // 添加最后一个索引
            if (lastIndexName != null && !lastIndexColumns.isEmpty()) {
                indexes.add(new IndexValObj()
                        .setName(lastIndexName)
                        .setIndexType(lastNonUnique ? "INDEX" : "UNIQUE")
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
     * 构建完整类型名称（带长度）
     */
    private String buildFullTypeName(String typeName, int columnSize) {
        if (typeName == null) {
            return "VARCHAR(255)";
        }
        // 对于需要长度的类型，添加长度信息
        String upperType = typeName.toUpperCase();
        if (upperType.contains("VARCHAR") || upperType.contains("CHAR")) {
            if (columnSize > 0 && !typeName.contains("(")) {
                return typeName + "(" + columnSize + ")";
            }
        }
        return typeName;
    }

    /**
     * 创建表
     */
    public void createTable(DsConfig dsConfig, String dbName, TableSchemaValObj tableSchema) {
        DatasourceType dsType = dsConfig.getDatasourceType();
        String url = buildUrlWithDb(dsConfig, dbName);
        
        StringBuilder sql = new StringBuilder();
        if (dsType == DatasourceType.MYSQL) {
            sql.append("CREATE TABLE `").append(tableSchema.getTableName()).append("` (\n");
        } else if (dsType == DatasourceType.POSTGRESQL) {
            sql.append("CREATE TABLE \"").append(tableSchema.getTableName()).append("\" (\n");
        } else {
            throw new SilentException("不支持的数据源类型: " + dsType);
        }

        // 添加字段
        List<String> columnDefs = new ArrayList<>();
        for (ColumnValObj column : tableSchema.getColumns()) {
            columnDefs.add(buildColumnDefinition(dsType, column));
        }

        // 添加主键（如果第一个索引是 PRIMARY）
        boolean hasPrimaryKey = tableSchema.getIndexes() != null && 
                tableSchema.getIndexes().stream().anyMatch(i -> "PRIMARY".equalsIgnoreCase(i.getName()));
        if (!hasPrimaryKey) {
            // 默认主键为 id
            if (dsType == DatasourceType.MYSQL) {
                columnDefs.add("PRIMARY KEY (`id`)");
            } else if (dsType == DatasourceType.POSTGRESQL) {
                columnDefs.add("PRIMARY KEY (\"id\")");
            }
        }

        sql.append(String.join(",\n", columnDefs));
        sql.append("\n)");

        // 添加表注释（MySQL）
        if (dsType == DatasourceType.MYSQL && tableSchema.getTableComment() != null && !tableSchema.getTableComment().isEmpty()) {
            sql.append(" COMMENT '").append(escapeString(tableSchema.getTableComment())).append("'");
        }

        executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), sql.toString());

        // 添加表注释（PostgreSQL）
        if (dsType == DatasourceType.POSTGRESQL && tableSchema.getTableComment() != null && !tableSchema.getTableComment().isEmpty()) {
            String commentSql = String.format("COMMENT ON TABLE \"%s\" IS '%s'", 
                    tableSchema.getTableName(), escapeString(tableSchema.getTableComment()));
            executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), commentSql);
        }

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
        DatasourceType dsType = dsConfig.getDatasourceType();
        
        // 如果表名变更，先重命名
        if (!oldTableName.equals(tableSchema.getTableName())) {
            String renameSql;
            if (dsType == DatasourceType.MYSQL) {
                renameSql = String.format("RENAME TABLE `%s` TO `%s`", oldTableName, tableSchema.getTableName());
            } else if (dsType == DatasourceType.POSTGRESQL) {
                renameSql = String.format("ALTER TABLE \"%s\" RENAME TO \"%s\"", oldTableName, tableSchema.getTableName());
            } else {
                throw new SilentException("不支持的数据源类型: " + dsType);
            }
            executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), renameSql);
        }

        // 获取旧表结构
        TableSchemaValObj oldSchema = getTableSchema(dsConfig, dbName, tableSchema.getTableName());

        // 修改表注释
        if (tableSchema.getTableComment() != null && !tableSchema.getTableComment().equals(oldSchema.getTableComment())) {
            String commentSql;
            if (dsType == DatasourceType.MYSQL) {
                commentSql = String.format("ALTER TABLE `%s` COMMENT '%s'", 
                        tableSchema.getTableName(), escapeString(tableSchema.getTableComment()));
            } else if (dsType == DatasourceType.POSTGRESQL) {
                commentSql = String.format("COMMENT ON TABLE \"%s\" IS '%s'", 
                        tableSchema.getTableName(), escapeString(tableSchema.getTableComment()));
            } else {
                throw new SilentException("不支持的数据源类型: " + dsType);
            }
            executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), commentSql);
        }

        // 字段变更 diff
        Map<String, ColumnValObj> oldColumnMap = oldSchema.getColumns() == null ? Map.of() :
                oldSchema.getColumns().stream().collect(Collectors.toMap(ColumnValObj::getName, Function.identity()));
        Map<String, ColumnValObj> newColumnMap = tableSchema.getColumns() == null ? Map.of() :
                tableSchema.getColumns().stream().collect(Collectors.toMap(ColumnValObj::getName, Function.identity()));

        // 1. 删除字段
        for (String oldColName : oldColumnMap.keySet()) {
            if (!newColumnMap.containsKey(oldColName)) {
                String dropColSql = buildDropColumnSql(dsType, tableSchema.getTableName(), oldColName);
                executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), dropColSql);
            }
        }

        // 2. 新增字段
        for (ColumnValObj newCol : tableSchema.getColumns()) {
            if (!oldColumnMap.containsKey(newCol.getName())) {
                String addColSql = buildAddColumnSql(dsType, tableSchema.getTableName(), newCol);
                executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), addColSql);
            }
        }

        // 3. 修改字段
        for (ColumnValObj newCol : tableSchema.getColumns()) {
            ColumnValObj oldCol = oldColumnMap.get(newCol.getName());
            if (oldCol != null && isColumnChanged(oldCol, newCol)) {
                String modifyColSql = buildModifyColumnSql(dsType, tableSchema.getTableName(), newCol);
                executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), modifyColSql);
            }
        }

        // 修改索引：先删除旧索引，再添加新索引
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
     * 判断字段是否发生变化
     */
    private boolean isColumnChanged(ColumnValObj oldCol, ColumnValObj newCol) {
        // 比较类型（忽略大小写和空格）
        if (!normalizeType(oldCol.getType()).equals(normalizeType(newCol.getType()))) return true;
        if (oldCol.getNullable() != newCol.getNullable()) return true;
        if (!equalsNullable(oldCol.getComment(), newCol.getComment())) return true;
        if (!equalsNullable(oldCol.getDefaultValue(), newCol.getDefaultValue())) return true;
        return false;
    }

    /**
     * 规范化类型字符串
     */
    private String normalizeType(String type) {
        if (type == null) return "";
        return type.toUpperCase().replaceAll("\\s+", " ").trim();
    }

    private boolean equalsNullable(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * 构建删除字段 SQL
     */
    private String buildDropColumnSql(DatasourceType dsType, String tableName, String colName) {
        if (dsType == DatasourceType.MYSQL) {
            return String.format("ALTER TABLE `%s` DROP COLUMN `%s`", tableName, colName);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            return String.format("ALTER TABLE \"%s\" DROP COLUMN \"%s\"", tableName, colName);
        }
        throw new SilentException("不支持的数据源类型: " + dsType);
    }

    /**
     * 构建新增字段 SQL
     */
    private String buildAddColumnSql(DatasourceType dsType, String tableName, ColumnValObj column) {
        String columnDef = buildColumnDefinitionForAlter(dsType, column);
        if (dsType == DatasourceType.MYSQL) {
            return String.format("ALTER TABLE `%s` ADD COLUMN %s", tableName, columnDef);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            return String.format("ALTER TABLE \"%s\" ADD COLUMN %s", tableName, columnDef);
        }
        throw new SilentException("不支持的数据源类型: " + dsType);
    }

    /**
     * 构建修改字段 SQL
     */
    private String buildModifyColumnSql(DatasourceType dsType, String tableName, ColumnValObj column) {
        if (dsType == DatasourceType.MYSQL) {
            String columnDef = buildColumnDefinitionForAlter(dsType, column);
            return String.format("ALTER TABLE `%s` MODIFY COLUMN %s", tableName, columnDef);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            // PostgreSQL 需要分开处理类型和 nullable
            String colName = column.getName();
            String dbType = column.getType();
            List<String> alterStmts = new ArrayList<>();
            alterStmts.add(String.format("ALTER TABLE \"%s\" ALTER COLUMN \"%s\" TYPE %s", tableName, colName, dbType));
            if (column.getNullable() != null) {
                if (column.getNullable()) {
                    alterStmts.add(String.format("ALTER TABLE \"%s\" ALTER COLUMN \"%s\" DROP NOT NULL", tableName, colName));
                } else {
                    alterStmts.add(String.format("ALTER TABLE \"%s\" ALTER COLUMN \"%s\" SET NOT NULL", tableName, colName));
                }
            }
            return String.join("; ", alterStmts);
        }
        throw new SilentException("不支持的数据源类型: " + dsType);
    }

    /**
     * 删除表
     */
    public void dropTable(DsConfig dsConfig, String dbName, String tableName) {
        DatasourceType dsType = dsConfig.getDatasourceType();
        String sql;
        if (dsType == DatasourceType.MYSQL) {
            sql = String.format("DROP TABLE IF EXISTS `%s`", tableName);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            sql = String.format("DROP TABLE IF EXISTS \"%s\"", tableName);
        } else {
            throw new SilentException("不支持的数据源类型: " + dsType);
        }
        String url = buildUrlWithDb(dsConfig, dbName);
        executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), sql);
    }

    /**
     * 创建索引
     */
    private void createIndex(DsConfig dsConfig, String dbName, String tableName, IndexValObj index) {
        String url = buildUrlWithDb(dsConfig, dbName);
        DatasourceType dsType = dsConfig.getDatasourceType();
        
        String columns;
        if (dsType == DatasourceType.MYSQL) {
            columns = index.getFieldNames().stream()
                    .map(col -> "`" + col + "`")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        } else if (dsType == DatasourceType.POSTGRESQL) {
            columns = index.getFieldNames().stream()
                    .map(col -> "\"" + col + "\"")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        } else {
            throw new SilentException("不支持的数据源类型: " + dsType);
        }

        String indexType = switch (index.getIndexType().toUpperCase()) {
            case "UNIQUE" -> "UNIQUE INDEX";
            case "FULLTEXT" -> {
                if (dsType != DatasourceType.MYSQL) {
                    throw new SilentException("PostgreSQL 不支持 FULLTEXT 索引，请使用 GIN 索引");
                }
                yield "FULLTEXT INDEX";
            }
            default -> "INDEX";
        };

        String sql;
        if (dsType == DatasourceType.MYSQL) {
            sql = String.format("ALTER TABLE `%s` ADD %s `%s` (%s)", 
                    tableName, indexType, index.getName(), columns);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            String unique = index.getIndexType().equalsIgnoreCase("UNIQUE") ? "UNIQUE " : "";
            sql = String.format("CREATE %sINDEX \"%s\" ON \"%s\" (%s)", 
                    unique, index.getName(), tableName, columns);
        } else {
            throw new SilentException("不支持的数据源类型: " + dsType);
        }
        
        executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), sql);
    }

    /**
     * 删除索引
     */
    private void dropIndex(DsConfig dsConfig, String dbName, String tableName, String indexName) {
        String url = buildUrlWithDb(dsConfig, dbName);
        DatasourceType dsType = dsConfig.getDatasourceType();
        
        String sql;
        if (dsType == DatasourceType.MYSQL) {
            sql = String.format("ALTER TABLE `%s` DROP INDEX `%s`", tableName, indexName);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            sql = String.format("DROP INDEX IF EXISTS \"%s\"", indexName);
        } else {
            throw new SilentException("不支持的数据源类型: " + dsType);
        }
        
        executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), sql);
    }

    /**
     * 构建字段定义（用于 CREATE TABLE）
     */
    private String buildColumnDefinition(DatasourceType dsType, ColumnValObj column) {
        StringBuilder def = new StringBuilder();
        String colName = column.getName();
        String dbType = column.getType();
        
        if (dsType == DatasourceType.MYSQL) {
            def.append("  `").append(colName).append("` ").append(dbType);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            def.append("  \"").append(colName).append("\" ").append(dbType);
        }

        if (Boolean.TRUE.equals(column.getNullable())) {
            def.append(" NULL");
        } else {
            def.append(" NOT NULL");
        }

        if (column.getDefaultValue() != null) {
            def.append(" DEFAULT '").append(escapeString(column.getDefaultValue())).append("'");
        }

        if (dsType == DatasourceType.MYSQL && Boolean.TRUE.equals(column.getAutoIncrement())) {
            def.append(" AUTO_INCREMENT");
        }

        // MySQL 字段注释
        if (dsType == DatasourceType.MYSQL && column.getComment() != null && !column.getComment().isEmpty()) {
            def.append(" COMMENT '").append(escapeString(column.getComment())).append("'");
        }

        return def.toString();
    }

    /**
     * 构建字段定义（用于 ALTER TABLE）
     */
    private String buildColumnDefinitionForAlter(DatasourceType dsType, ColumnValObj column) {
        StringBuilder def = new StringBuilder();
        String colName = column.getName();
        String dbType = column.getType();
        
        if (dsType == DatasourceType.MYSQL) {
            def.append("`").append(colName).append("` ").append(dbType);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            def.append("\"").append(colName).append("\" ").append(dbType);
        }

        if (Boolean.TRUE.equals(column.getNullable())) {
            def.append(" NULL");
        } else {
            def.append(" NOT NULL");
        }

        if (column.getDefaultValue() != null) {
            def.append(" DEFAULT '").append(escapeString(column.getDefaultValue())).append("'");
        }

        return def.toString();
    }

    /**
     * 获取驱动类名
     */
    private String getDriverClass(DatasourceType dsType) {
        return switch (dsType) {
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL -> "org.postgresql.Driver";
            case ICEBERG -> throw new SilentException("ICEBERG 不支持直接 JDBC 连接");
        };
    }

    /**
     * 构建带数据库的 URL
     */
    private String buildUrlWithDb(DsConfig dsConfig, String dbName) {
        DatasourceType dsType = dsConfig.getDatasourceType();
        
        if (dsType == DatasourceType.MYSQL || dsType == DatasourceType.POSTGRESQL) {
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
            // 支持多条 SQL 语句（PostgreSQL ALTER COLUMN 需要）
            if (sql.contains("; ")) {
                for (String s : sql.split("; ")) {
                    if (!s.isEmpty()) {
                        stmt.executeUpdate(s);
                    }
                }
            } else {
                stmt.executeUpdate(sql);
            }
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

    // ==================== 工厂方法 ====================

    /**
     * 根据数据源类型创建对应的 ColumnValObj 子类实例
     */
    public ColumnValObj createColumnValObj(DatasourceType dsType) {
        return switch (dsType) {
            case MYSQL -> new MysqlColumnValObj().setDatabaseType("MYSQL");
            case POSTGRESQL -> new PgsqlColumnValObj().setDatabaseType("POSTGRESQL");
            default -> throw new SilentException("不支持的数据源类型: " + dsType);
        };
    }

    // ==================== 类型枚举获取方法 ====================

    /**
     * 获取 MySQL 字段类型列表
     */
    public List<MysqlColumnType> getMysqlColumnTypes() {
        return List.of(MysqlColumnType.values());
    }

    /**
     * 获取 PostgreSQL 字段类型列表
     */
    public List<PostgresColumnType> getPostgresColumnTypes() {
        return List.of(PostgresColumnType.values());
    }
}