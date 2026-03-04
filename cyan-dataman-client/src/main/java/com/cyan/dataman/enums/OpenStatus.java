package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 描述: 开启状态枚举
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum OpenStatus {
    OPEN("OPEN", "开启"),
    CLOSE("CLOSE", "关闭");

    private final String code;
    private final String desc;

    public static OpenStatus getByCode(String code) {
        for (OpenStatus value : OpenStatus.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
