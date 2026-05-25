package com.cyan.dataman.infra.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 关联推荐向量搜索结果
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class RelationVectorSearchResult {

    /**
     * 表ID
     */
    private String tableId;

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
     * 相似度分数
     */
    private Double score;
}
