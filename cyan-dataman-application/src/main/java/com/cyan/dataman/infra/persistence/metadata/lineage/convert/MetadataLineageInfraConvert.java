package com.cyan.dataman.infra.persistence.metadata.lineage.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageEdge;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageNode;
import com.cyan.dataman.infra.persistence.metadata.lineage.dos.MetadataLineageEdgeDO;
import com.cyan.dataman.infra.persistence.metadata.lineage.dos.MetadataLineageNodeDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据血缘基础设施层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataLineageInfraConvert {

    /**
     * 单例
     */
    MetadataLineageInfraConvert INSTANCE = Mappers.getMapper(MetadataLineageInfraConvert.class);

    /**
     * 节点 DO 转领域对象
     */
    @Mapping(target = "id", expression = "java(nodeDO.getId() == null ? null : String.valueOf(nodeDO.getId()))")
    MetadataLineageNode toNode(MetadataLineageNodeDO nodeDO);

    /**
     * 节点领域对象转 DO
     */
    @Mapping(target = "id", expression = "java(node.getId() == null || node.getId().isBlank() ? null : Long.valueOf(node.getId()))")
    MetadataLineageNodeDO toNodeDO(MetadataLineageNode node);

    /**
     * 边 DO 转领域对象
     */
    @Mapping(target = "id", expression = "java(edgeDO.getId() == null ? null : String.valueOf(edgeDO.getId()))")
    MetadataLineageEdge toEdge(MetadataLineageEdgeDO edgeDO);

    /**
     * 边领域对象转 DO
     */
    @Mapping(target = "id", expression = "java(edge.getId() == null || edge.getId().isBlank() ? null : Long.valueOf(edge.getId()))")
    MetadataLineageEdgeDO toEdgeDO(MetadataLineageEdge edge);

    /**
     * 节点 DO 列表转领域列表
     */
    List<MetadataLineageNode> toNodes(List<MetadataLineageNodeDO> nodeDOs);

    /**
     * 边 DO 列表转领域列表
     */
    List<MetadataLineageEdge> toEdges(List<MetadataLineageEdgeDO> edgeDOs);
}
