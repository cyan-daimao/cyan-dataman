package com.cyan.dataman.domain.metadata.lineage.query;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 字段血缘查询对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataFieldLineageQuery {

    /**
     * catalog
     */
    private String catalog;

    /**
     * schema
     */
    private String schema;

    /**
     * 表名
     */
    private String table;

    /**
     * 字段名
     */
    private String column;

    /**
     * 最大查询深度
     */
    private Integer maxDepth;

    /**
     * 构建字段节点唯一键
     */
    public String fieldNodeKey() {
        return "field:" + catalog + "." + schema + "." + table + "." + column;
    }
}
