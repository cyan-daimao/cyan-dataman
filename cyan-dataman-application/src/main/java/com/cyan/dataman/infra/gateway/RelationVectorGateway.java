package com.cyan.dataman.infra.gateway;

import com.cyan.dataman.infra.gateway.model.RelationVectorDocument;
import com.cyan.dataman.infra.gateway.model.RelationVectorSearchResult;

import java.util.List;

/**
 * 关联推荐向量网关
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface RelationVectorGateway {

    /**
     * 是否可用
     *
     * @return 是否可用
     */
    boolean available();

    /**
     * 默认召回数量
     *
     * @return 召回数量
     */
    int topK();

    /**
     * 确保集合存在
     *
     * @param dimension 向量维度
     */
    void ensureCollection(int dimension);

    /**
     * 写入向量文档
     *
     * @param document 向量文档
     */
    void upsert(RelationVectorDocument document);

    /**
     * 删除向量文档
     *
     * @param pointId 点ID
     */
    void delete(String pointId);

    /**
     * 搜索相似表
     *
     * @param vector 查询向量
     * @param limit  返回数量
     * @return 搜索结果
     */
    List<RelationVectorSearchResult> search(List<Double> vector, int limit);
}
