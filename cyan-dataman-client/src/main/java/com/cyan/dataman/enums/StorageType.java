package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储类型
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
public enum StorageType {

    MYSQL("MYSQL", "MYSQL"),
    POSTGRESQL("POSTGRESQL", "POSTGRESQL"),
    ;

    private final String code;

    private final String desc;


    public static StorageType getByCode(String code) {
        for (StorageType value : StorageType.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
