package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 写入模式
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum WriteMode {

    OVERWRITE("OVERWRITE", "覆盖"),
    APPEND("APPEND", "追加"),
    ;
    private final String code;
    private final String desc;


    public static WriteMode getByCode(String code) {
        for (WriteMode value : WriteMode.values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}
