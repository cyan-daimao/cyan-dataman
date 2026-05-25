package com.cyan.dataman.application.metadata;

import com.cyan.dataman.application.metadata.bo.RelationVectorIndexResultBO;
import com.cyan.dataman.domain.metadata.MetadataTable;

import java.util.List;

/**
 * 关联推荐向量索引服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface RelationVectorIndexService {

    /**
     * 是否可用
     *
     * @return 是否可用
     */
    boolean available();

    /**
     * 重建所有表索引
     *
     * @return 重建结果
     */
    RelationVectorIndexResultBO rebuildAll();

    /**
     * 重建单表索引
     *
     * @param tableId 表ID
     * @return 重建结果
     */
    RelationVectorIndexResultBO rebuildOne(String tableId);

    /**
     * 写入单表索引
     *
     * @param table 元数据表
     */
    void upsert(MetadataTable table);

    /**
     * 删除单表索引
     *
     * @param table 元数据表
     */
    void delete(MetadataTable table);

    /**
     * 删除单表索引
     *
     * @param tableId 表ID
     * @return 删除结果
     */
    RelationVectorIndexResultBO deleteOne(String tableId);

    /**
     * 搜索相似表
     *
     * @param current 当前表
     * @return 相似表列表
     */
    List<MetadataTable> searchSimilarTables(MetadataTable current);
}
