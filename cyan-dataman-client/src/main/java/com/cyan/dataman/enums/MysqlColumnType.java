package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MySQL 字段类型枚举
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
public enum MysqlColumnType {

    // 整数类型
    TINYINT("TINYINT", "微整型(1字节)"),
    SMALLINT("SMALLINT", "小整型(2字节)"),
    MEDIUMINT("MEDIUMINT", "中整型(3字节)"),
    INT("INT", "整型(4字节)"),
    BIGINT("BIGINT", "大整型(8字节)"),

    // 浮点类型
    FLOAT("FLOAT", "单精度浮点"),
    DOUBLE("DOUBLE", "双精度浮点"),
    DECIMAL("DECIMAL", "高精度数值"),

    // 字符串类型
    CHAR("CHAR", "定长字符串"),
    VARCHAR("VARCHAR", "变长字符串"),
    TEXT("TEXT", "长文本"),
    MEDIUMTEXT("MEDIUMTEXT", "中等长度文本"),
    LONGTEXT("LONGTEXT", "长文本"),

    // 二进制类型
    BLOB("BLOB", "二进制大对象"),
    MEDIUMBLOB("MEDIUMBLOB", "中等长度二进制"),
    LONGBLOB("LONGBLOB", "长二进制"),

    // 日期时间类型
    DATE("DATE", "日期"),
    TIME("TIME", "时间"),
    DATETIME("DATETIME", "日期时间"),
    TIMESTAMP("TIMESTAMP", "时间戳"),
    YEAR("YEAR", "年份"),

    // 其他类型
    BOOLEAN("BOOLEAN", "布尔类型"),
    JSON("JSON", "JSON类型"),
    ENUM("ENUM", "枚举类型"),
    SET("SET", "集合类型"),
    ;

    private final String code;
    private final String desc;

    public static MysqlColumnType getByCode(String code) {
        if (code == null) {
            return null;
        }
        String upperCode = code.toUpperCase();
        for (MysqlColumnType value : values()) {
            if (value.code.equals(upperCode)) {
                return value;
            }
        }
        // 处理带括号的类型，如 VARCHAR(255)
        for (MysqlColumnType value : values()) {
            if (upperCode.startsWith(value.code)) {
                return value;
            }
        }
        return null;
    }
}
