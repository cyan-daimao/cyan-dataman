package com.cyan.dataman.infra.persistence.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.MetadataSubject;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataSubjectDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 元数据主题转换
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataSubjectInfraConvert {
    MetadataSubjectInfraConvert INSTANCE = Mappers.getMapper(MetadataSubjectInfraConvert.class);

    MetadataSubject toMetaDataSubject(MetadataSubjectDO metaDataSubjectDO);

    MetadataSubjectDO toMetaDataSubjectDO(MetadataSubject metadataSubject);
}
