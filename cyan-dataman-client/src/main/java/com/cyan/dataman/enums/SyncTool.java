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
    ;

    private final String code;
}
