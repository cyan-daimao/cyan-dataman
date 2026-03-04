package com.cyan.dataman.infra.persistence.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface MetadataTableInfraConvert {

    MetadataTableInfraConvert INSTANCE = Mappers.getMapper(MetadataTableInfraConvert.class);

    MetadataTable toMetadataTable(MetadataTableDO bo);
}
