package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PostgreSQL 字段类型枚举
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
public enum PostgresColumnType {

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

    public static PostgresColumnType getByCode(String code) {
        if (code == null) {
            return null;
        }
        String upperCode = code.toUpperCase();
        for (PostgresColumnType value : values()) {
            if (value.code.equals(upperCode)) {
                return value;
            }
        }
        // 处理带括号的类型，如 VARCHAR(255)
        for (PostgresColumnType value : values()) {
            if (upperCode.startsWith(value.code.split(" ")[0])) {
                return value;
            }
        }
        return null;
    }
}
