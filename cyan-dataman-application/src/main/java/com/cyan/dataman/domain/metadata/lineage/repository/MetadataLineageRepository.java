package com.cyan.dataman.domain.metadata.lineage.repository;

import com.cyan.dataman.domain.metadata.lineage.MetadataLineageEdge;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageNode;

import java.util.Collection;
import java.util.List;

/**
 * 元数据血缘仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataLineageRepository {

    /**
     * 保存或更新血缘节点
     */
    MetadataLineageNode saveOrUpdateNode(MetadataLineageNode node);

    /**
     * 保存血缘边
     */
    MetadataLineageEdge saveEdge(MetadataLineageEdge edge);

    /**
     * 删除指定来源的血缘边
     */
    void deleteEdgesByServiceAndRefId(String serviceName, String refId);

    /**
     * 根据节点唯一键批量查询节点
     */
    List<MetadataLineageNode> findNodesByKeys(Collection<String> nodeKeys);

    /**
     * 查询目标节点的指定类型入边
     */
    List<MetadataLineageEdge> findEdgesByTargetAndType(String targetKey, String edgeType);

    /**
     * 查询源节点的指定类型出边
     */
    List<MetadataLineageEdge> findEdgesBySourceAndType(String sourceKey, String edgeType);

    /**
     * 查询源节点的全部出边
     */
    List<MetadataLineageEdge> findEdgesBySourceKeys(Collection<String> sourceKeys);
}
