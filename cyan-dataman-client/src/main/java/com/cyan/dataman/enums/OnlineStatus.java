package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 线上状态
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
public enum OnlineStatus {

    ONLINE("ONLINE", "上线"),
    OFFLINE("OFFLINE", "下线"),
    ;
    private final String code;
    private final String desc;

    public static OnlineStatus getByCode(String code) {
        for (OnlineStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
