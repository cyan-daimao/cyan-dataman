package com.cyan.dataman.adapter.metadata.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据表信息agent传输
 *
 * @author cy.Y
 * @since v1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class MetadataTableAgentDTO {
    /**
     * 元数据表所属schema
     */
    private String schema;

    /**
     * 元数据表名称
     */
    private String name;

    /**
     * 元数据表所属主题名称
     */
    private String subjectName;

    /**
     * 元数据表描述
     */
    private String comment;
}
