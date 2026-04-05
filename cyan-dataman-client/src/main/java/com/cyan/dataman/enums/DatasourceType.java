package com.cyan.dataman.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据源类型
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum DatasourceType {
    MYSQL("MYSQL"),

    POSTGRESQL("POSTGRESQL"),

    ICEBERG("ICEBERG"),
    ;

    private final String code;


    public static DatasourceType getByCode(String code) {

        return switch (code) {
            case "MYSQL","jdbc-mysql" -> MYSQL;
            case "POSTGRESQL","jdbc-postgresql" -> POSTGRESQL;
            case "ICEBERG","lakehouse-iceberg" -> ICEBERG;
            default -> null;
        };
    }
}
