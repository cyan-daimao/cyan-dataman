package com.cyan.dataman.adapter.metadata.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据表字段DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataColumnAgentDTO {


    /**
     * 字段名
     */
    private String col;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 字段注释
     */
    private String comment;

    /**
     * 可空
     */
    private Boolean nullable;

}
