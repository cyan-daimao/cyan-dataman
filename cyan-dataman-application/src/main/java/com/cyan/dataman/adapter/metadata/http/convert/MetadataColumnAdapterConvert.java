package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataColumnDTO;
import com.cyan.dataman.application.metadata.bo.MetadataColumnBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据字段适配层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataColumnAdapterConvert {
    MetadataColumnAdapterConvert INSTANCE = Mappers.getMapper(MetadataColumnAdapterConvert.class);

    MetadataColumnDTO toMetadataColumnDTO(MetadataColumnBO metadataColumnBO);

    List<MetadataColumnDTO> toMetadataColumnDTOList(List<MetadataColumnBO> metadataColumnBOs);
}
