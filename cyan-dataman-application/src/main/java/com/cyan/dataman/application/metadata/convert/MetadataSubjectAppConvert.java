package com.cyan.dataman.application.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.application.metadata.bo.MetadataSubjectBO;
import com.cyan.dataman.application.metadata.cmd.MetadataSubjectCmd;
import com.cyan.dataman.domain.metadata.MetadataSubject;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface MetadataSubjectAppConvert {
    MetadataSubjectAppConvert INSTANCE = Mappers.getMapper(MetadataSubjectAppConvert.class);

    MetadataSubjectBO toMetadataSubjectBO(MetadataSubject metaDataSubject);

    MetadataSubject toMetadataSubject(MetadataSubjectCmd cmd);
}
