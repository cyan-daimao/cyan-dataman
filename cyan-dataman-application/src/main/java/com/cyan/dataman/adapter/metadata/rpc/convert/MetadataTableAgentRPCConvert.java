package com.cyan.dataman.adapter.metadata.rpc.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataColumnAgentDTO;
import com.cyan.dataman.application.metadata.bo.MetadataColumnBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 智能体 RPC 层转化
 *
 * @author cy.Y
 * @since v1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface MetadataTableAgentRPCConvert {

    MetadataTableAgentRPCConvert INSTANCE = Mappers.getMapper(MetadataTableAgentRPCConvert.class);

    MetadataColumnAgentDTO toMetadataColumnAgentDTO(MetadataColumnBO metadataColumnBO);

    default List<MetadataColumnAgentDTO> toMetadataColumnAgentDTOList(List<MetadataColumnBO> metadataColumnBOList) {
        return metadataColumnBOList.stream().map(this::toMetadataColumnAgentDTO).toList();
    }
}
