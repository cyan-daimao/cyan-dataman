package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataSubjectDTO;
import com.cyan.dataman.application.metadata.bo.MetadataSubjectBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataSubjectAdapterConvert {

    MetadataSubjectAdapterConvert INSTANCE = Mappers.getMapper(MetadataSubjectAdapterConvert.class);

    MetadataSubjectDTO toDMetadataSubjectDTO(MetadataSubjectBO metaDataSubjectBO);
}
