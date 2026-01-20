package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 字段类型
 *
 * @author cy.Y
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum FieldType {
    BOOLEAN("BOOLEAN", "布尔类型"),
    INTEGER("INTEGER", "整型"),
    LONG("LONG", "长整型"),
    FLOAT("FLOAT", "浮点型"),
    DOUBLE("DOUBLE", "双精度型"),
    DATE("DATE", "日期类型"),
    TIME("TIME", "时间类型"),
    TIMESTAMP("TIMESTAMP", "时间戳类型"),
    TIMESTAMP_NANO("TIMESTAMP_NANO", "时间戳类型(纳秒精度)"),
    STRING("STRING", "字符串类型"),
    UUID("UUID", "UUID类型"),
    FIXED("FIXED", "固定长度字节数组类型"),
    BINARY("BINARY", "二进制类型"),
    BYTE("BYTE", "字节类型"),
    DECIMAL("DECIMAL", "十进制类型"),
    GEOMETRY("GEOMETRY", "几何类型"),
    GEOGRAPHY("GEOGRAPHY", "地理位置类型"),
    STRUCT("STRUCT", "结构体类型"),
    LIST("LIST", "列表类型"),
    MAP("MAP", "映射类型"),
    VARIANT("VARIANT", "变体类型"),
    UNKNOWN("UNKNOWN", "未知类型");

    private final String code;

    private final String desc;

    public FieldType getByCode(String code) {
        for (FieldType fieldType : FieldType.values()) {
            if (fieldType.code.equals(code)) {
                return fieldType;
            }
        }
        return null;
    }

}
