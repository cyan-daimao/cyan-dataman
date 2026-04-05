package com.cyan.dataman.domain.metadata.valobj;

import com.cyan.dataman.enums.ColumnDataType;
import com.cyan.dataman.enums.MysqlColumnType;
import com.cyan.dataman.enums.PostgresColumnType;
import com.cyan.dataman.enums.SecretLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 字段信息
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ColumnValObj {
    /**
     * 字段名称
     */
    @NotBlank(message = "字段名称不能为空")
    private String name;

    /**
     * 字段类型（原始数据库类型字符串，如 VARCHAR(255), INT, BIGINT 等）
     */
    @NotBlank(message = "字段类型不能为空")
    private String type;

    /**
     * 字段注释
     */
    @NotBlank(message = "字段注释不能为空")
    private String comment;

    /**
     * 字段是否为空
     */
    private Boolean nullable;

    /**
     * 字段是否自增
     */
    private Boolean autoIncrement;

    /**
     * 字段默认值
     */
    private String defaultValue;

    /**
     * 字段密级
     */
    private SecretLevel secretLevel;

    /**
     * 精度（如 DECIMAL(10,2) 中的 10，或 VARCHAR(255) 中的 255）
     */
    private Integer precision;

    /**
     * 小数位数（仅 DECIMAL 类型使用）
     */
    private Integer scale;

    /**
     * 获取 MySQL 字段类型枚举
     */
    public MysqlColumnType getMysqlType() {
        return MysqlColumnType.getByCode(this.type);
    }

    /**
     * 获取 PostgreSQL 字段类型枚举
     */
    public PostgresColumnType getPostgresType() {
        return PostgresColumnType.getByCode(this.type);
    }

    /**
     * 获取通用字段类型枚举（用于 Iceberg/Gravitino 兼容）
     * 从 type 字符串解析
     */
    public ColumnDataType getColumnDataType() {
        // 先尝试直接匹配
        ColumnDataType direct = ColumnDataType.getByCode(this.type);
        if (direct != null) {
            return direct;
        }
        // 尝试从 MySQL 类型映射
        MysqlColumnType mysqlType = getMysqlType();
        if (mysqlType != null) {
            return mapMysqlToColumnType(mysqlType);
        }
        // 尝试从 PostgreSQL 类型映射
        PostgresColumnType pgType = getPostgresType();
        if (pgType != null) {
            return mapPostgresToColumnType(pgType);
        }
        // 默认返回 STRING
        return ColumnDataType.STRING;
    }

    /**
     * 使用 ColumnDataType 枚举设置字段类型
     */
    public ColumnValObj setColumnType(ColumnDataType columnDataType) {
        this.type = columnDataType.getCode();
        return this;
    }

    private ColumnDataType mapMysqlToColumnType(MysqlColumnType mysqlType) {
        return switch (mysqlType) {
            case TINYINT, BOOLEAN -> ColumnDataType.BOOLEAN;
            case SMALLINT, MEDIUMINT, INT -> ColumnDataType.INTEGER;
            case BIGINT -> ColumnDataType.LONG;
            case FLOAT -> ColumnDataType.FLOAT;
            case DOUBLE, DECIMAL -> ColumnDataType.DOUBLE;
            case DATE -> ColumnDataType.DATE;
            case TIME -> ColumnDataType.TIME;
            case DATETIME, TIMESTAMP -> ColumnDataType.TIMESTAMP;
            case BLOB, MEDIUMBLOB, LONGBLOB -> ColumnDataType.BINARY;
            default -> ColumnDataType.STRING;
        };
    }

    private ColumnDataType mapPostgresToColumnType(PostgresColumnType pgType) {
        return switch (pgType) {
            case BOOLEAN -> ColumnDataType.BOOLEAN;
            case SMALLINT, INTEGER -> ColumnDataType.INTEGER;
            case BIGINT -> ColumnDataType.LONG;
            case REAL -> ColumnDataType.FLOAT;
            case DOUBLE_PRECISION, NUMERIC, DECIMAL -> ColumnDataType.DOUBLE;
            case DATE -> ColumnDataType.DATE;
            case TIME, TIME_WITH_TIME_ZONE -> ColumnDataType.TIME;
            case TIMESTAMP -> ColumnDataType.TIMESTAMP;
            case TIMESTAMP_WITH_TIME_ZONE -> ColumnDataType.TIMESTAMP_TZ;
            case BYTEA -> ColumnDataType.BINARY;
            case UUID -> ColumnDataType.UUID;
            default -> ColumnDataType.STRING;
        };
    }
}