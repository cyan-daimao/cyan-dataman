package com.cyan.dataman.adapter.http.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 字段
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableFieldDTO {
    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段类型
     */
    private String type;
}
