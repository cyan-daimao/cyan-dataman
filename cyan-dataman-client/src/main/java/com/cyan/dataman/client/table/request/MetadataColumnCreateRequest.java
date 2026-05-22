package com.cyan.dataman.client.table.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据表字段创建请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MetadataColumnCreateRequest {

    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段类型（如 STRING, BIGINT, TIMESTAMP 等）
     */
    private String type;

    /**
     * 字段注释
     */
    private String comment;

    /**
     * 是否可为空
     */
    private Boolean nullable;
}
