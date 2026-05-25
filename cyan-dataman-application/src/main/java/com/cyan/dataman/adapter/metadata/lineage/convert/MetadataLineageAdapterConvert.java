package com.cyan.dataman.adapter.metadata.lineage.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.lineage.dto.MetadataFieldLineageDTO;
import com.cyan.dataman.application.metadata.lineage.bo.MetadataFieldLineageBO;
import com.cyan.dataman.application.metadata.lineage.cmd.MetadataLineageSyncCmd;
import com.cyan.dataman.client.lineage.dto.MetadataLineageEdgeDTO;
import com.cyan.dataman.client.lineage.dto.MetadataLineageNodeDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据血缘适配层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataLineageAdapterConvert {

    /**
     * 单例
     */
    MetadataLineageAdapterConvert INSTANCE = Mappers.getMapper(MetadataLineageAdapterConvert.class);

    /**
     * 字段血缘 BO 转 DTO
     */
    MetadataFieldLineageDTO toDTO(MetadataFieldLineageBO bo);

    /**
     * 节点 DTO 转命令
     */
    MetadataLineageSyncCmd.LineageNodeCmd toNodeCmd(MetadataLineageNodeDTO dto);

    /**
     * 边 DTO 转命令
     */
    MetadataLineageSyncCmd.LineageEdgeCmd toEdgeCmd(MetadataLineageEdgeDTO dto);

    /**
     * 节点 DTO 列表转命令列表
     */
    List<MetadataLineageSyncCmd.LineageNodeCmd> toNodeCmds(List<MetadataLineageNodeDTO> dtos);

    /**
     * 边 DTO 列表转命令列表
     */
    List<MetadataLineageSyncCmd.LineageEdgeCmd> toEdgeCmds(List<MetadataLineageEdgeDTO> dtos);
}
