package com.cyan.dataman.infra.util;

import com.cyan.dataman.domain.ds.valobj.ColumnValObj;

/**
 * Debezium 数据库类型到 Flink SQL 类型映射器
 * <p>
 * 将 MySQL / PostgreSQL 的原始类型映射为 Flink SQL 类型定义，
 * 用于 CDC Sink 表的 DDL 生成和 JSON_VALUE 字段提取。
 *
 * @author cy.Y
 * @since 1.0.0
 */
public class DebeziumTypeMapper {

    /**
     * 将数据库字段转换为 Flink SQL 类型定义
     *
     * @param column 数据源字段值对象
     * @return Flink SQL 类型字符串，如 INT、BIGINT、DECIMAL(10,2)、STRING 等
     */
    public static String toFlinkSqlType(ColumnValObj column) {
        String dbType = column.getType();
        if (dbType == null) {
            return "STRING";
        }
        String upper = dbType.toUpperCase();
        Integer precision = column.getPrecision();
        Integer scale = column.getScale();

        return switch (upper) {
            case "TINYINT", "SMALLINT", "MEDIUMINT", "INT", "INTEGER" -> "INT";
            case "BIGINT" -> "BIGINT";
            case "FLOAT", "REAL" -> "FLOAT";
            case "DOUBLE", "DOUBLE PRECISION" -> "DOUBLE";
            case "DECIMAL", "NUMERIC" -> {
                int p = precision != null && precision > 0 ? precision : 38;
                int s = scale != null && scale >= 0 ? scale : 0;
                yield "DECIMAL(" + p + "," + s + ")";
            }
            case "BOOLEAN" -> "BOOLEAN";
            // 时间类型映射为 TIMESTAMP，Flink 会自动 CAST ISO 8601 字符串
            case "DATETIME", "TIMESTAMP",
                 "TIMESTAMP WITH TIME ZONE", "TIMESTAMPTZ" -> "TIMESTAMP(3)";
            // DATE / TIME 保持 STRING，避免精度丢失
            case "DATE", "TIME",
                 "TIME WITH TIME ZONE", "TIMETZ" -> "STRING";
            // 字符串和二进制类型
            case "CHAR", "VARCHAR", "TEXT", "LONGTEXT", "MEDIUMTEXT", "TINYTEXT",
                 "JSON", "JSONB", "ENUM", "SET",
                 "INET", "CIDR", "MACADDR", "UUID", "XML",
                 "GEOMETRY", "POINT", "LINESTRING", "POLYGON", "MULTIPOINT",
                 "MULTILINESTRING", "MULTIPOLYGON", "GEOMETRYCOLLECTION" -> "STRING";
            case "BINARY", "VARBINARY", "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB", "BYTEA" -> "BYTES";
            default -> "STRING";
        };
    }

    /**
     * 判断该 Flink 类型是否需要 CAST
     *
     * @param flinkType Flink SQL 类型
     * @return true 表示需要 CAST
     */
    public static boolean needsCast(String flinkType) {
        return flinkType != null && !"STRING".equals(flinkType);
    }

    /**
     * 构建 JSON_VALUE 提取表达式
     *
     * @param columnName 字段名
     * @param flinkType  Flink SQL 类型
     * @return 如 CAST(JSON_VALUE(_raw_json, '$.payload.after.id') AS BIGINT) AS `id`
     */
    public static String buildExtractExpr(String columnName, String flinkType) {
        String jsonPath = "JSON_VALUE(_raw_json, '$.payload.after." + escapeJsonPath(columnName) + "')";
        String quotedName = "`" + columnName + "`";
        if (needsCast(flinkType)) {
            return "CAST(" + jsonPath + " AS " + flinkType + ") AS " + quotedName;
        }
        return jsonPath + " AS " + quotedName;
    }

    /**
     * 转义 JSON Path 中的字段名
     * <p>
     * 目前 Debezium 生成的 JSON 字段名一般不需要复杂转义，
     * 若字段名包含单引号则进行简单处理。
     */
    private static String escapeJsonPath(String columnName) {
        return columnName.replace("'", "\\'");
    }
}
