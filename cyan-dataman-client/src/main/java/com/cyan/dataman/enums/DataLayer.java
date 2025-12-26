package com.cyan.dataman.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数仓数据库
 *
 * @author cy.Y
 * @version 1.0.0
 */

@Getter
@AllArgsConstructor
public enum DataLayer {
    ODS("ODS", "数据源层"),
    DWD("DWD", "数据明细层"),
    DWM("DWM", "数据中间层"),
    DWS("DWS", "数据服务层"),
    ADS("ADS", "数据应用层"),
    DIM("DIM", "维度表"),
    ;

    private final String code;
    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static DataLayer getByCode(String code) {
        for (DataLayer value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }

}
