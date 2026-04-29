package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataColumnAgentDTO;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataTableAgentDTO;
import com.cyan.dataman.application.metadata.bo.MetadataColumnBO;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 *
 * 智能体转化
 *
 * @author cy.Y
 * @since v1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface MetadataTableAgentAdapterConvert {

    MetadataTableAgentAdapterConvert INSTANCE = Mappers.getMapper(MetadataTableAgentAdapterConvert.class);

    @Mapping(target = "schema", source = "layerCode")
    MetadataTableAgentDTO toMetadataTableAgentDTO(MetadataTableBO metadataTableBO);

    default List<MetadataTableAgentDTO> toMetadataTableAgentDTOList(List<MetadataTableBO> metadataTableBOList){
        return metadataTableBOList.stream().map(this::toMetadataTableAgentDTO).toList();
    }


    MetadataColumnAgentDTO toMetadataColumnAgentDTO(MetadataColumnBO metadataColumnBO);

    default List<MetadataColumnAgentDTO> toMetadataColumnAgentDTOList(List<MetadataColumnBO> metadataColumnBOList){
        return metadataColumnBOList.stream().map(this::toMetadataColumnAgentDTO).toList();
    }
}
