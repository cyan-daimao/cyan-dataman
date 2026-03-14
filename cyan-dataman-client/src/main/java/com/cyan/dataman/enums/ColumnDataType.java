package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 字段数据类型 - 与Gravitino/Iceberg类型对应
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
public enum ColumnDataType {

    BOOLEAN("BOOLEAN", "布尔类型"),
    INTEGER("INTEGER", "整型(4字节)"),
    LONG("LONG", "长整型(8字节)"),
    FLOAT("FLOAT", "单精度浮点"),
    DOUBLE("DOUBLE", "双精度浮点"),
    DECIMAL("DECIMAL", "高精度数值"),
    STRING("STRING", "字符串"),
    DATE("DATE", "日期"),
    TIMESTAMP("TIMESTAMP", "时间戳(无时区)"),
    TIMESTAMP_TZ("TIMESTAMP_TZ", "时间戳(有时区)"),
    TIME("TIME", "时间"),
    BINARY("BINARY", "二进制"),
    UUID("UUID", "UUID"),
    ;

    private final String code;
    private final String desc;

    public static ColumnDataType getByCode(String code) {
        for (ColumnDataType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
