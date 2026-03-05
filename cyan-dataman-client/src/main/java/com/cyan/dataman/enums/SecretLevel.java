package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * 秘密等级
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum SecretLevel {

    L1("L1", "L1"),
    L2("L2", "L2"),
    L3("L3", "L3"),
    L4("L4", "L4");

    private final String code;
    private final String desc;

    public static SecretLevel getByCode(String code) {
        for (SecretLevel value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
