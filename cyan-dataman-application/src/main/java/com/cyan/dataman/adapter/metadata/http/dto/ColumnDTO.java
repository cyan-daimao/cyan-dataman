package com.cyan.dataman.adapter.metadata.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.gravitino.rel.types.Type;

/**
 * 字段信息
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ColumnDTO {
    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段类型
     */
    private Type.Name type;

    /**
     * 字段注释
     */
    private String comment;

    /**
     * 字段是否为空
     */
    private Boolean nullable;

    /**
     * 字段是否自增
     */
    private Boolean autoIncrement;

    /**
     * 字段默认值
     */
    private String defaultValue;
}
