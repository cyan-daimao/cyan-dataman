package com.cyan.dataman.domain.ds.valobj;

import com.cyan.dataman.enums.PostgresColumnType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * PostgreSQL 字段信息
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@JsonTypeName("POSTGRESQL")
public class PgsqlColumnValObj extends ColumnValObj {

    /**
     * PostgreSQL 字段类型枚举
     */
    @Getter
    public enum PgsqlType {
        // 整数类型
        SMALLINT("SMALLINT", "小整型(2字节)"),
        INTEGER("INTEGER", "整型(4字节)"),
        BIGINT("BIGINT", "大整型(8字节)"),
        SMALLSERIAL("SMALLSERIAL", "自增小整型"),
        SERIAL("SERIAL", "自增整型"),
        BIGSERIAL("BIGSERIAL", "自增大整型"),

        // 浮点类型
        REAL("REAL", "单精度浮点"),
        DOUBLE_PRECISION("DOUBLE PRECISION", "双精度浮点"),
        NUMERIC("NUMERIC", "高精度数值"),
        DECIMAL("DECIMAL", "高精度数值"),

        // 字符串类型
        CHAR("CHAR", "定长字符串"),
        VARCHAR("VARCHAR", "变长字符串"),
        TEXT("TEXT", "长文本"),

        // 二进制类型
        BYTEA("BYTEA", "字节数组"),

        // 日期时间类型
        DATE("DATE", "日期"),
        TIME("TIME", "时间"),
        TIME_WITH_TIME_ZONE("TIME WITH TIME ZONE", "带时区时间"),
        TIMESTAMP("TIMESTAMP", "时间戳(无时区)"),
        TIMESTAMP_WITH_TIME_ZONE("TIMESTAMP WITH TIME ZONE", "时间戳(带时区)"),

        // 布尔类型
        BOOLEAN("BOOLEAN", "布尔类型"),

        // JSON 类型
        JSON("JSON", "JSON类型"),
        JSONB("JSONB", "JSON二进制类型"),

        // UUID 类型
        UUID("UUID", "UUID类型"),

        // 数组类型
        ARRAY("ARRAY", "数组类型"),

        // 网络地址类型
        CIDR("CIDR", "CIDR网络地址"),
        INET("INET", "IP地址"),
        MACADDR("MACADDR", "MAC地址"),

        // 几何类型
        POINT("POINT", "点"),
        LINE("LINE", "线"),
        LSEG("LSEG", "线段"),
        BOX("BOX", "矩形"),
        PATH("PATH", "路径"),
        POLYGON("POLYGON", "多边形"),
        CIRCLE("CIRCLE", "圆"),
        ;

        private final String code;
        private final String desc;

        PgsqlType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public static PgsqlType getByCode(String code) {
            if (code == null) {
                return null;
            }
            String upperCode = code.toUpperCase();
            for (PgsqlType value : values()) {
                if (value.code.equals(upperCode)) {
                    return value;
                }
            }
            // 处理带括号的类型，如 VARCHAR(255)
            for (PgsqlType value : values()) {
                if (upperCode.startsWith(value.code.split(" ")[0])) {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * 数组维度（PostgreSQL 支持数组类型）
     */
    private Integer arrayDimensions;

    /**
     * 时区标识
     */
    private Boolean withTimeZone;

    @Override
    public String getTypeEnumCode() {
        PgsqlType pgsqlType = getPgsqlTypeEnum();
        return pgsqlType != null ? pgsqlType.getCode() : this.type;
    }

    /**
     * 获取 PostgreSQL 字段类型枚举
     */
    public PgsqlType getPgsqlTypeEnum() {
        return PgsqlType.getByCode(this.type);
    }

    /**
     * 使用 PgsqlType 枚举设置字段类型
     */
    public PgsqlColumnValObj setPgsqlType(PgsqlType pgsqlType) {
        this.type = pgsqlType.getCode();
        return this;
    }

    /**
     * 使用 client 模块的 PostgresColumnType 枚举设置字段类型
     */
    public PgsqlColumnValObj setPostgresColumnType(PostgresColumnType postgresColumnType) {
        this.type = postgresColumnType.getCode();
        return this;
    }

    /**
     * 获取 client 模块的 PostgresColumnType 枚举
     */
    public PostgresColumnType getPostgresColumnType() {
        return PostgresColumnType.getByCode(this.type);
    }

    /**
     * 判断是否为整数类型
     */
    public boolean isIntegerType() {
        PgsqlType pgsqlType = getPgsqlTypeEnum();
        if (pgsqlType == null) {
            return false;
        }
        return pgsqlType == PgsqlType.SMALLINT
                || pgsqlType == PgsqlType.INTEGER
                || pgsqlType == PgsqlType.BIGINT
                || pgsqlType == PgsqlType.SMALLSERIAL
                || pgsqlType == PgsqlType.SERIAL
                || pgsqlType == PgsqlType.BIGSERIAL;
    }

    /**
     * 判断是否为字符串类型
     */
    public boolean isStringType() {
        PgsqlType pgsqlType = getPgsqlTypeEnum();
        if (pgsqlType == null) {
            return false;
        }
        return pgsqlType == PgsqlType.CHAR
                || pgsqlType == PgsqlType.VARCHAR
                || pgsqlType == PgsqlType.TEXT;
    }

    /**
     * 判断是否为时间类型
     */
    public boolean isDateTimeType() {
        PgsqlType pgsqlType = getPgsqlTypeEnum();
        if (pgsqlType == null) {
            return false;
        }
        return pgsqlType == PgsqlType.DATE
                || pgsqlType == PgsqlType.TIME
                || pgsqlType == PgsqlType.TIME_WITH_TIME_ZONE
                || pgsqlType == PgsqlType.TIMESTAMP
                || pgsqlType == PgsqlType.TIMESTAMP_WITH_TIME_ZONE;
    }

    /**
     * 判断是否为自增类型
     */
    public boolean isSerialType() {
        PgsqlType pgsqlType = getPgsqlTypeEnum();
        if (pgsqlType == null) {
            return false;
        }
        return pgsqlType == PgsqlType.SMALLSERIAL
                || pgsqlType == PgsqlType.SERIAL
                || pgsqlType == PgsqlType.BIGSERIAL;
    }

    /**
     * 判断是否为数组类型
     */
    public boolean isArrayType() {
        return arrayDimensions != null && arrayDimensions > 0;
    }
}
