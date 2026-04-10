package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 同步工具类型
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum SyncTool {
    SPARK("SPARK"),
    FLINK("FLINK"),
    DEBEZIUM("DEBEZIUM"),
    ;

    private final String code;

    public static SyncTool getByCode(String code) {
        for (SyncTool value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
