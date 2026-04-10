package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CDC 连接器运行状态
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum RunningStatus {

    INIT("INIT", "初始化"),
    RUNNING("RUNNING", "运行中"),
    STOP("STOP", "已停止"),
    SUCCESS("SUCCESS", "成功"),
    ERROR("ERROR", "异常"),
    ;

    private final String code;
    private final String description;

    public static RunningStatus getByCode(String code) {
        for (RunningStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
