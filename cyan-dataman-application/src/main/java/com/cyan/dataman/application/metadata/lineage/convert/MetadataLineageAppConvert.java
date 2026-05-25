package com.cyan.dataman.application.metadata.lineage.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.application.metadata.lineage.cmd.MetadataLineageSyncCmd;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageEdge;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageNode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据血缘应用层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataLineageAppConvert {

    /**
     * 单例
     */
    MetadataLineageAppConvert INSTANCE = Mappers.getMapper(MetadataLineageAppConvert.class);

    /**
     * 节点命令转领域对象
     */
    MetadataLineageNode toNode(MetadataLineageSyncCmd.LineageNodeCmd cmd);

    /**
     * 边命令转领域对象
     */
    MetadataLineageEdge toEdge(MetadataLineageSyncCmd.LineageEdgeCmd cmd);

    /**
     * 节点命令列表转领域列表
     */
    List<MetadataLineageNode> toNodes(List<MetadataLineageSyncCmd.LineageNodeCmd> cmds);

    /**
     * 边命令列表转领域列表
     */
    List<MetadataLineageEdge> toEdges(List<MetadataLineageSyncCmd.LineageEdgeCmd> cmds);
}
