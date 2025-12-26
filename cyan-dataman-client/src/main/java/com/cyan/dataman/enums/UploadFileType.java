package com.cyan.dataman.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 上传文件类型
 * @author cy.Y
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum UploadFileType {

    CSV("CSV","CSV文件"),
    EXCEL("EXCEL","EXCEL文件"),
    ;

    private final String code;

    private final String desc;

    @JsonCreator
    public static UploadFileType getByCode(String code) {
        if (code.equalsIgnoreCase("xls")|| code.equalsIgnoreCase("xlsx")){
            return EXCEL;
        }
        for (UploadFileType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}
