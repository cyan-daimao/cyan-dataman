package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataTableDTO;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface MetadataTableAdapterConvert {
    MetadataTableAdapterConvert INSTANCE = Mappers.getMapper(MetadataTableAdapterConvert.class);

    MetadataTableDTO toMetadataTableDTO(MetadataTableBO metadataTableDO);
}
