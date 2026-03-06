package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 字段数据类型
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
public enum ColumnDataType {

    NUMBER("NUMBER", "数字"),
    TEXT("NUMBER", "文本"),
    DATETIME("DATETIME", "时间"),
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
