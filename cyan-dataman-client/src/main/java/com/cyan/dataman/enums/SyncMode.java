package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 同步模式
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum SyncMode {
    OVERWRITE("OVERWRITE", "覆盖"),
    APPEND("APPEND", "追加"),
    ;

    private final String code;
    private final String description;
}
