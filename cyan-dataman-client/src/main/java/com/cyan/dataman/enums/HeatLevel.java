package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 热度
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum HeatLevel {

    HOT("HOT", "最热"),
    NORMAL("NORMAL", "普通"),
    COLD("COLD", "最冷"),
    ZOMBIE("ZOMBIE", "僵尸");

    private final String code;
    private final String desc;

    public static HeatLevel getByCode(String code) {
        for (HeatLevel heatLevel : HeatLevel.values()) {
            if (heatLevel.code.equals(code)) {
                return heatLevel;
            }
        }
        return null;
    }
}
