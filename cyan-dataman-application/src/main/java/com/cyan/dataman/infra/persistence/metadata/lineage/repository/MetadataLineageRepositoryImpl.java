package com.cyan.dataman.infra.persistence.metadata.lineage.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageEdge;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageNode;
import com.cyan.dataman.domain.metadata.lineage.repository.MetadataLineageRepository;
import com.cyan.dataman.infra.persistence.metadata.lineage.convert.MetadataLineageInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.lineage.dos.MetadataLineageEdgeDO;
import com.cyan.dataman.infra.persistence.metadata.lineage.dos.MetadataLineageNodeDO;
import com.cyan.dataman.infra.persistence.metadata.lineage.mappers.MetadataLineageEdgeMapper;
import com.cyan.dataman.infra.persistence.metadata.lineage.mappers.MetadataLineageNodeMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 元数据血缘仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class MetadataLineageRepositoryImpl implements MetadataLineageRepository {

    private final MetadataLineageNodeMapper nodeMapper;
    private final MetadataLineageEdgeMapper edgeMapper;

    public MetadataLineageRepositoryImpl(MetadataLineageNodeMapper nodeMapper,
                                         MetadataLineageEdgeMapper edgeMapper) {
        this.nodeMapper = nodeMapper;
        this.edgeMapper = edgeMapper;
    }

    /**
     * 保存或更新血缘节点
     */
    @Override
    public MetadataLineageNode saveOrUpdateNode(MetadataLineageNode node) {
        MetadataLineageNodeDO existing = nodeMapper.selectOne(new LambdaQueryWrapper<MetadataLineageNodeDO>()
                .eq(MetadataLineageNodeDO::getNodeKey, node.getNodeKey())
                .last("limit 1"));
        MetadataLineageNodeDO nodeDO = MetadataLineageInfraConvert.INSTANCE.toNodeDO(node);
        nodeDO.setUpdatedAt(LocalDateTime.now());
        if (existing == null) {
            nodeDO.setCreatedAt(LocalDateTime.now());
            nodeMapper.insert(nodeDO);
            return MetadataLineageInfraConvert.INSTANCE.toNode(nodeDO);
        }
        nodeDO.setId(existing.getId());
        nodeDO.setCreatedAt(existing.getCreatedAt());
        nodeMapper.updateById(nodeDO);
        return MetadataLineageInfraConvert.INSTANCE.toNode(nodeMapper.selectById(existing.getId()));
    }

    /**
     * 保存血缘边
     */
    @Override
    public MetadataLineageEdge saveEdge(MetadataLineageEdge edge) {
        MetadataLineageEdgeDO edgeDO = MetadataLineageInfraConvert.INSTANCE.toEdgeDO(edge);
        edgeDO.setCreatedAt(LocalDateTime.now());
        edgeDO.setUpdatedAt(LocalDateTime.now());
        edgeMapper.insert(edgeDO);
        return MetadataLineageInfraConvert.INSTANCE.toEdge(edgeDO);
    }

    /**
     * 删除指定来源的血缘边
     */
    @Override
    public void deleteEdgesByServiceAndRefId(String serviceName, String refId) {
        edgeMapper.delete(new LambdaQueryWrapper<MetadataLineageEdgeDO>()
                .eq(MetadataLineageEdgeDO::getServiceName, serviceName)
                .eq(MetadataLineageEdgeDO::getRefId, refId));
    }

    /**
     * 根据节点唯一键批量查询节点
     */
    @Override
    public List<MetadataLineageNode> findNodesByKeys(Collection<String> nodeKeys) {
        if (nodeKeys == null || nodeKeys.isEmpty()) {
            return List.of();
        }
        List<MetadataLineageNodeDO> nodeDOs = nodeMapper.selectList(new LambdaQueryWrapper<MetadataLineageNodeDO>()
                .in(MetadataLineageNodeDO::getNodeKey, nodeKeys));
        return MetadataLineageInfraConvert.INSTANCE.toNodes(Optional.ofNullable(nodeDOs).orElse(List.of()));
    }

    /**
     * 查询目标节点的指定类型入边
     */
    @Override
    public List<MetadataLineageEdge> findEdgesByTargetAndType(String targetKey, String edgeType) {
        List<MetadataLineageEdgeDO> edgeDOs = edgeMapper.selectList(new LambdaQueryWrapper<MetadataLineageEdgeDO>()
                .eq(MetadataLineageEdgeDO::getTargetKey, targetKey)
                .eq(MetadataLineageEdgeDO::getEdgeType, edgeType));
        return MetadataLineageInfraConvert.INSTANCE.toEdges(Optional.ofNullable(edgeDOs).orElse(List.of()));
    }

    /**
     * 查询源节点的指定类型出边
     */
    @Override
    public List<MetadataLineageEdge> findEdgesBySourceAndType(String sourceKey, String edgeType) {
        List<MetadataLineageEdgeDO> edgeDOs = edgeMapper.selectList(new LambdaQueryWrapper<MetadataLineageEdgeDO>()
                .eq(MetadataLineageEdgeDO::getSourceKey, sourceKey)
                .eq(MetadataLineageEdgeDO::getEdgeType, edgeType));
        return MetadataLineageInfraConvert.INSTANCE.toEdges(Optional.ofNullable(edgeDOs).orElse(List.of()));
    }

    /**
     * 查询源节点的全部出边
     */
    @Override
    public List<MetadataLineageEdge> findEdgesBySourceKeys(Collection<String> sourceKeys) {
        if (sourceKeys == null || sourceKeys.isEmpty()) {
            return List.of();
        }
        List<MetadataLineageEdgeDO> edgeDOs = edgeMapper.selectList(new LambdaQueryWrapper<MetadataLineageEdgeDO>()
                .in(MetadataLineageEdgeDO::getSourceKey, sourceKeys));
        return MetadataLineageInfraConvert.INSTANCE.toEdges(Optional.ofNullable(edgeDOs).orElse(List.of()));
    }
}
