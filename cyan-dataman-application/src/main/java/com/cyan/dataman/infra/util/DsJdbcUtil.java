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
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        DatasourceType dsType = dsConfig.getDatasourceType();
        try (Connection conn = getConnection(dsConfig)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getCatalogs();
            
            // 获取数据库注释（不同数据源查询方式不同）
            Map<String, String> dbComments = getDatabaseComments(conn, dsType);
            
            while (rs.next()) {
                String dbName = rs.getString("TABLE_CAT");
                String comment = dbComments.getOrDefault(dbName, dbName);
                databases.add(new DatabaseValObj()
                        .setName(dbName)
                        .setComment(comment));
            }
        } catch (SQLException e) {
            throw new SilentException("获取数据库列表失败: " + e.getMessage());
        }
        return databases;
    }

    /**
     * 获取数据库注释（通过查询系统表）
     */
    private Map<String, String> getDatabaseComments(Connection conn, DatasourceType dsType) {
        Map<String, String> comments = new HashMap<>();
        try {
            String sql;
            if (dsType == DatasourceType.MYSQL) {
                // MySQL 数据库注释存储在 information_schema.SCHEMATA 中，但该表没有注释列
                // MySQL 实际上不支持数据库级别的注释，这里返回数据库名作为注释
                return comments;
            } else if (dsType == DatasourceType.POSTGRESQL) {
                sql = "SELECT datname, shobj_description(oid, 'pg_database') as comment FROM pg_database WHERE datistemplate = false";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        String dbName = rs.getString("datname");
                        String comment = rs.getString("comment");
                        if (comment != null && !comment.isEmpty()) {
                            comments.put(dbName, comment);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("获取数据库注释失败: {}", e.getMessage());
        }
        return comments;
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
    public List<TableSchemaValObj> listTables(DsConfig dsConfig, String dbName) {
        List<TableSchemaValObj> tables = new ArrayList<>();
        String url = buildUrlWithDb(dsConfig, dbName);
        try (Connection conn = DriverManager.getConnection(url, dsConfig.getUsername(), dsConfig.getPassword())) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(dbName, null, "%", new String[]{"TABLE"});
            
            // 获取表注释（通过查询 information_schema）
            Map<String, String> tableComments = getTableComments(conn, dsConfig.getDatasourceType(), dbName);
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                // 优先使用额外查询获取的注释，否则使用 JDBC 返回的
                String comment = tableComments.get(tableName);
                if (comment == null || comment.isEmpty()) {
                    comment = rs.getString("REMARKS");
                }
                tables.add(new TableSchemaValObj()
                        .setTableName(tableName)
                        .setTableComment(comment != null ? comment : ""));
            }
        } catch (SQLException e) {
            throw new SilentException("获取表列表失败: " + e.getMessage());
        }
        return tables;
    }

    /**
     * 获取表注释（通过查询 information_schema）
     */
    private Map<String, String> getTableComments(Connection conn, DatasourceType dsType, String dbName) {
        Map<String, String> comments = new HashMap<>();
        try {
            String sql;
            if (dsType == DatasourceType.MYSQL) {
                sql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'";
            } else if (dsType == DatasourceType.POSTGRESQL) {
                sql = "SELECT c.relname as table_name, obj_description(c.oid) as table_comment " +
                        "FROM pg_class c " +
                        "JOIN pg_namespace n ON c.relnamespace = n.oid " +
                        "WHERE n.nspname = 'public' AND c.relkind = 'r'";
            } else {
                return comments;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (dsType == DatasourceType.MYSQL) {
                    stmt.setString(1, dbName);
                }
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    String comment = rs.getString("table_comment");
                    if (comment != null && !comment.isEmpty()) {
                        comments.put(tableName, comment);
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("获取表注释失败: {}", e.getMessage());
        }
        return comments;
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
            
            // 对于 MySQL，额外查询字段注释（JDBC 驱动可能无法正确返回）
            Map<String, String> columnComments = getColumnComments(conn, dsConfig.getDatasourceType(), dbName, tableName);
            
            while (columnRs.next()) {
                String dbTypeName = columnRs.getString("TYPE_NAME");
                int columnSize = columnRs.getInt("COLUMN_SIZE");
                int decimalDigits = columnRs.getInt("DECIMAL_DIGITS");
                String columnName = columnRs.getString("COLUMN_NAME");
                
                // 解析类型名称（去掉括号中的长度信息）
                String pureTypeName = extractPureTypeName(dbTypeName);
                
                // 设置精度
                Integer precision = null;
                Integer scale = null;
                if (columnSize > 0) {
                    // 整数类型和字符串类型都有精度（长度）
                    if (isIntegerType(pureTypeName) || needsLength(pureTypeName)) {
                        precision = columnSize;
                    }
                    // DECIMAL 类型有精度和标度
                    if (needsPrecision(pureTypeName)) {
                        precision = columnSize;
                        if (decimalDigits > 0) {
                            scale = decimalDigits;
                        }
                    }
                }
                
                // 优先使用额外查询获取的注释，否则使用 JDBC 返回的
                String comment = columnComments.get(columnName);
                if (comment == null || comment.isEmpty()) {
                    comment = columnRs.getString("REMARKS");
                }
                
                // 判断是否有无符号标识（MySQL）
                boolean unsigned = false;
                if (dsConfig.getDatasourceType() == DatasourceType.MYSQL && dbTypeName != null) {
                    unsigned = dbTypeName.toUpperCase().contains("UNSIGNED");
                }
                
                ColumnValObj column = createColumnValObj(dsConfig.getDatasourceType())
                        .setName(columnName)
                        .setType(pureTypeName)
                        .setComment(comment != null ? comment : "")
                        .setNullable(columnRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                        .setDefaultValue(columnRs.getString("COLUMN_DEF"))
                        .setPrecision(precision)
                        .setScale(scale);
                
                // 设置 MySQL 特有属性
                if (column instanceof MysqlColumnValObj mysqlColumn && unsigned) {
                    mysqlColumn.setUnsigned(true);
                    mysqlColumn.setType(mysqlColumn.getType().replace("UNSIGNED", "").trim());
                }
                
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
     * 提取纯类型名称（去掉括号中的长度信息）
     * 例如：VARCHAR(255) -> VARCHAR, DECIMAL(10,2) -> DECIMAL
     */
    private String extractPureTypeName(String typeName) {
        if (typeName == null) {
            return "VARCHAR";
        }
        int parenIndex = typeName.indexOf('(');
        if (parenIndex > 0) {
            return typeName.substring(0, parenIndex);
        }
        return typeName;
    }

    /**
     * 获取字段注释（通过查询 information_schema）
     */
    private Map<String, String> getColumnComments(Connection conn, DatasourceType dsType, String dbName, String tableName) {
        Map<String, String> comments = new HashMap<>();
        try {
            String sql;
            if (dsType == DatasourceType.MYSQL) {
                sql = "SELECT COLUMN_NAME, COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
            } else if (dsType == DatasourceType.POSTGRESQL) {
                sql = "SELECT a.attname as column_name, col_description(a.attrelid, a.attnum) as column_comment " +
                        "FROM pg_attribute a " +
                        "JOIN pg_class c ON a.attrelid = c.oid " +
                        "JOIN pg_namespace n ON c.relnamespace = n.oid " +
                        "WHERE n.nspname = 'public' AND c.relname = ? AND a.attnum > 0 AND NOT a.attisdropped";
            } else {
                return comments;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (dsType == DatasourceType.MYSQL) {
                    stmt.setString(1, dbName);
                    stmt.setString(2, tableName);
                } else {
                    stmt.setString(1, tableName);
                }
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String comment = rs.getString("column_comment");
                    if (comment != null && !comment.isEmpty()) {
                        comments.put(columnName, comment);
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("获取字段注释失败: {}", e.getMessage());
        }
        return comments;
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

        // 2. 新增字段（按新顺序处理，MySQL需要指定位置）
        for (int i = 0; i < tableSchema.getColumns().size(); i++) {
            ColumnValObj newCol = tableSchema.getColumns().get(i);
            if (!oldColumnMap.containsKey(newCol.getName())) {
                String afterColumn = i > 0 ? tableSchema.getColumns().get(i - 1).getName() : null;
                String addColSql = buildAddColumnSql(dsType, tableSchema.getTableName(), newCol, afterColumn);
                executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), addColSql);
            }
        }

        // 3. 修改字段（包括属性变化和顺序变化）
        // 构建旧字段顺序映射：字段名 -> 在旧表中的索引位置
        Map<String, Integer> oldColumnOrder = new LinkedHashMap<>();
        if (oldSchema.getColumns() != null) {
            for (int i = 0; i < oldSchema.getColumns().size(); i++) {
                oldColumnOrder.put(oldSchema.getColumns().get(i).getName(), i);
            }
        }
        
        // 按新顺序处理字段，检测属性变化和顺序变化
        for (int i = 0; i < tableSchema.getColumns().size(); i++) {
            ColumnValObj newCol = tableSchema.getColumns().get(i);
            ColumnValObj oldCol = oldColumnMap.get(newCol.getName());
            if (oldCol != null) {
                boolean attributeChanged = isColumnChanged(oldCol, newCol);
                boolean orderChanged = false;
                
                // 检测顺序变化：比较当前字段在新旧表中的位置
                Integer oldIndex = oldColumnOrder.get(newCol.getName());
                if (oldIndex != null && oldIndex != i) {
                    orderChanged = true;
                }
                
                // 如果属性或顺序发生变化，执行 MODIFY COLUMN
                if (attributeChanged || orderChanged) {
                    String afterColumn = i > 0 ? tableSchema.getColumns().get(i - 1).getName() : null;
                    String modifyColSql = buildModifyColumnSql(dsType, tableSchema.getTableName(), newCol, afterColumn);
                    executeUpdate(url, dsConfig.getUsername(), dsConfig.getPassword(), modifyColSql);
                }
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
    private String buildAddColumnSql(DatasourceType dsType, String tableName, ColumnValObj column, String afterColumn) {
        String columnDef = buildColumnDefinitionForAlter(dsType, column);
        if (dsType == DatasourceType.MYSQL) {
            String positionClause = "";
            if (afterColumn == null) {
                positionClause = " FIRST";
            } else {
                positionClause = " AFTER `" + afterColumn + "`";
            }
            return String.format("ALTER TABLE `%s` ADD COLUMN %s%s", tableName, columnDef, positionClause);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            String sql = String.format("ALTER TABLE \"%s\" ADD COLUMN %s", tableName, columnDef);
            // PostgreSQL 字段注释需要单独语句
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                sql += String.format("; COMMENT ON COLUMN \"%s\".\"%s\" IS '%s'", 
                        tableName, column.getName(), escapeString(column.getComment()));
            }
            return sql;
        }
        throw new SilentException("不支持的数据源类型: " + dsType);
    }

    /**
     * 构建修改字段 SQL
     */
    private String buildModifyColumnSql(DatasourceType dsType, String tableName, ColumnValObj column, String afterColumn) {
        if (dsType == DatasourceType.MYSQL) {
            String columnDef = buildColumnDefinitionForAlter(dsType, column);
            String positionClause = "";
            if (afterColumn == null) {
                positionClause = " FIRST";
            } else {
                positionClause = " AFTER `" + afterColumn + "`";
            }
            return String.format("ALTER TABLE `%s` MODIFY COLUMN %s%s", tableName, columnDef, positionClause);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            // PostgreSQL 需要分开处理类型和 nullable
            String colName = column.getName();
            String dbType = buildFullTypeFromColumn(column);
            List<String> alterStmts = new ArrayList<>();
            alterStmts.add(String.format("ALTER TABLE \"%s\" ALTER COLUMN \"%s\" TYPE %s", tableName, colName, dbType));
            if (column.getNullable() != null) {
                if (column.getNullable()) {
                    alterStmts.add(String.format("ALTER TABLE \"%s\" ALTER COLUMN \"%s\" DROP NOT NULL", tableName, colName));
                } else {
                    alterStmts.add(String.format("ALTER TABLE \"%s\" ALTER COLUMN \"%s\" SET NOT NULL", tableName, colName));
                }
            }
            // PostgreSQL 字段注释
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                alterStmts.add(String.format("COMMENT ON COLUMN \"%s\".\"%s\" IS '%s'", 
                        tableName, colName, escapeString(column.getComment())));
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

    // ==================== SQL 执行接口 ====================

    /**
     * 执行 DQL 查询语句
     * @param dsConfig 数据源配置
     * @param dbName 数据库名
     * @param sql 查询SQL
     * @param limit 结果行数限制，null表示不限制
     * @return 查询结果，包含列名和数据行
     */
    public Map<String, Object> executeQuery(DsConfig dsConfig, String dbName, String sql, Integer limit) {
        String url = buildUrlWithDb(dsConfig, dbName);
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(url, dsConfig.getUsername(), dsConfig.getPassword());
             Statement stmt = conn.createStatement()) {
            
            // 设置最大行数限制
            if (limit != null && limit > 0) {
                stmt.setMaxRows(limit);
            }
            
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 获取列名
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }
            
            // 获取数据行
            int rowCount = 0;
            int maxRows = limit != null && limit > 0 ? limit : Integer.MAX_VALUE;
            while (rs.next() && rowCount < maxRows) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    row.put(columns.get(i - 1), value);
                }
                rows.add(row);
                rowCount++;
            }
            
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("rowCount", rows.size());
        } catch (SQLException e) {
            throw new SilentException("执行查询SQL失败: " + e.getMessage() + ", SQL: " + sql);
        }
        
        return result;
    }

    /**
     * 执行 DML 语句（INSERT/UPDATE/DELETE）
     * @param dsConfig 数据源配置
     * @param dbName 数据库名
     * @param sql DML SQL语句
     * @return 影响的行数
     */
    public int executeDml(DsConfig dsConfig, String dbName, String sql) {
        String url = buildUrlWithDb(dsConfig, dbName);
        
        try (Connection conn = DriverManager.getConnection(url, dsConfig.getUsername(), dsConfig.getPassword());
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new SilentException("执行DML SQL失败: " + e.getMessage() + ", SQL: " + sql);
        }
    }

    /**
     * 执行 SQL 语句（自动判断 DQL 或 DML）
     * @param dsConfig 数据源配置
     * @param dbName 数据库名
     * @param sql SQL语句
     * @param limit 结果行数限制（仅查询有效），null表示不限制
     * @return 执行结果
     */
    public Map<String, Object> executeSql(DsConfig dsConfig, String dbName, String sql, Integer limit) {
        String url = buildUrlWithDb(dsConfig, dbName);
        String trimmedSql = sql.trim().toUpperCase();
        
        // 判断是否为查询语句
        boolean isQuery = trimmedSql.startsWith("SELECT") || 
                          trimmedSql.startsWith("SHOW") || 
                          trimmedSql.startsWith("DESC") ||
                          trimmedSql.startsWith("DESCRIBE") ||
                          trimmedSql.startsWith("EXPLAIN") ||
                          trimmedSql.startsWith("WITH");
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try (Connection conn = DriverManager.getConnection(url, dsConfig.getUsername(), dsConfig.getPassword());
             Statement stmt = conn.createStatement()) {
            
            if (isQuery) {
                // 执行查询
                if (limit != null && limit > 0) {
                    // 仅对 SELECT / WITH 语句自动追加 LIMIT，避免 SHOW/DESC/EXPLAIN 语法错误
                    boolean isDql = trimmedSql.startsWith("SELECT") || trimmedSql.startsWith("WITH");
                    if (isDql && !trimmedSql.contains("LIMIT")) {
                        String cleanSql = sql.trim();
                        if (cleanSql.endsWith(";")) {
                            cleanSql = cleanSql.substring(0, cleanSql.length() - 1);
                        }
                        sql = cleanSql + " LIMIT " + limit;
                    }
                    stmt.setMaxRows(limit);
                }
                
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnLabel(i));
                }
                
                List<Map<String, Object>> rows = new ArrayList<>();
                int rowCount = 0;
                int maxRows = limit != null && limit > 0 ? limit : Integer.MAX_VALUE;
                while (rs.next() && rowCount < maxRows) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        row.put(columns.get(i - 1), value);
                    }
                    rows.add(row);
                    rowCount++;
                }
                
                result.put("isQuery", true);
                result.put("columns", columns);
                result.put("rows", rows);
                result.put("rowCount", rows.size());
            } else {
                // 执行更新
                int affectedRows = stmt.executeUpdate(sql);
                result.put("isQuery", false);
                result.put("affectedRows", affectedRows);
            }
        } catch (SQLException e) {
            throw new SilentException("执行SQL失败: " + e.getMessage() + ", SQL: " + sql);
        }
        
        return result;
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
        String dbType = buildFullTypeFromColumn(column);
        
        if (dsType == DatasourceType.MYSQL) {
            def.append("  `").append(colName).append("` ").append(dbType);
            // MySQL 无符号
            if (column instanceof MysqlColumnValObj mysqlColumn && Boolean.TRUE.equals(mysqlColumn.getUnsigned())) {
                def.append(" UNSIGNED");
            }
        } else if (dsType == DatasourceType.POSTGRESQL) {
            def.append("  \"").append(colName).append("\" ").append(dbType);
        }

        // MySQL 中显式写 NULL 是语法错误，所以 nullable=true 时不写（默认就是 NULL）
        if (Boolean.FALSE.equals(column.getNullable())) {
            def.append(" NOT NULL");
        }

        // 只有 defaultValue 非空时才添加 DEFAULT 子句
        if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
            String defaultValue = column.getDefaultValue();
            // 判断是否为SQL函数或关键字（如 CURRENT_TIMESTAMP、NULL 等），这些不需要引号包裹
            if (isSqlFunctionOrKeyword(defaultValue)) {
                def.append(" DEFAULT ").append(defaultValue);
            } else {
                def.append(" DEFAULT '").append(escapeString(defaultValue)).append("'");
            }
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
        String dbType = buildFullTypeFromColumn(column);
        
        if (dsType == DatasourceType.MYSQL) {
            def.append("`").append(colName).append("` ").append(dbType);
            // MySQL 无符号
            if (column instanceof MysqlColumnValObj mysqlColumn && Boolean.TRUE.equals(mysqlColumn.getUnsigned())) {
                def.append(" UNSIGNED");
            }
        } else if (dsType == DatasourceType.POSTGRESQL) {
            def.append("\"").append(colName).append("\" ").append(dbType);
        }

        // MySQL 中显式写 NULL 是语法错误，所以 nullable=true 时不写（默认就是 NULL）
        if (Boolean.FALSE.equals(column.getNullable())) {
            def.append(" NOT NULL");
        }

        // 只有 defaultValue 非空时才添加 DEFAULT 子句
        if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
            String defaultValue = column.getDefaultValue();
            // 判断是否为SQL函数或关键字（如 CURRENT_TIMESTAMP、NULL 等），这些不需要引号包裹
            if (isSqlFunctionOrKeyword(defaultValue)) {
                def.append(" DEFAULT ").append(defaultValue);
            } else {
                def.append(" DEFAULT '").append(escapeString(defaultValue)).append("'");
            }
        }

        // MySQL 字段注释
        if (dsType == DatasourceType.MYSQL && column.getComment() != null && !column.getComment().isEmpty()) {
            def.append(" COMMENT '").append(escapeString(column.getComment())).append("'");
        }

        return def.toString();
    }

    /**
     * 根据 ColumnValObj 构建完整的类型定义（包含长度/精度）
     */
    private String buildFullTypeFromColumn(ColumnValObj column) {
        String type = column.getType();
        if (type == null || type.isEmpty()) {
            return "VARCHAR(255)";
        }
        
        String upperType = type.toUpperCase();
        Integer precision = column.getPrecision();
        Integer scale = column.getScale();
        
        // 如果类型已经包含括号，直接返回
        if (type.contains("(")) {
            return type;
        }
        
        // 整数类型（只有精度，无标度）
        if (isIntegerType(upperType)) {
            if (precision != null && precision > 0) {
                return type + "(" + precision + ")";
            }
            return type;
        }
        
        // 字符串类型（需要长度）
        if (needsLength(upperType)) {
            if (precision != null && precision > 0) {
                return type + "(" + precision + ")";
            }
            return type + "(255)";
        }
        
        // DECIMAL 类型（精度+标度）
        if (needsPrecision(upperType)) {
            if (precision != null && precision > 0) {
                if (scale != null && scale > 0) {
                    return type + "(" + precision + "," + scale + ")";
                }
                return type + "(" + precision + ")";
            }
            return type + "(10,2)";
        }
        
        return type;
    }

    /**
     * 判断是否为整数类型
     */
    private boolean isIntegerType(String upperType) {
        return upperType.equals("BIGINT") 
                || upperType.equals("INT")
                || upperType.equals("INTEGER")
                || upperType.equals("SMALLINT")
                || upperType.equals("TINYINT")
                || upperType.equals("MEDIUMINT");
    }

    /**
     * 判断类型是否需要长度参数
     */
    private boolean needsLength(String upperType) {
        return upperType.equals("VARCHAR") 
                || upperType.equals("CHAR")
                || upperType.equals("NVARCHAR")
                || upperType.equals("NCHAR")
                || upperType.equals("VARBINARY")
                || upperType.equals("BINARY");
    }

    /**
     * 判断类型是否需要精度参数
     */
    private boolean needsPrecision(String upperType) {
        return upperType.equals("DECIMAL") 
                || upperType.equals("NUMERIC");
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

    /**
     * 判断默认值是否为SQL函数或关键字
     * 这些值不需要用单引号包裹，如 CURRENT_TIMESTAMP、NULL 等
     */
    private boolean isSqlFunctionOrKeyword(String defaultValue) {
        String upper = defaultValue.toUpperCase().trim();
        // MySQL和PostgreSQL常见的SQL函数和关键字
        return upper.equals("CURRENT_TIMESTAMP") ||
               upper.equals("CURRENT_DATE") ||
               upper.equals("CURRENT_TIME") ||
               upper.equals("NULL") ||
               upper.equals("NOW()") ||
               upper.equals("UUID()") ||
               // 匹配带有精度的函数，如 CURRENT_TIMESTAMP(6)
               upper.matches("CURRENT_TIMESTAMP\\(\\d+\\)") ||
               upper.matches("CURRENT_TIME\\(\\d+\\)") ||
               // PostgreSQL 的序列函数
               upper.matches("NEXTVAL\\('.*'\\)") ||
               // 其他以函数形式结尾的表达式
               upper.endsWith("()");
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