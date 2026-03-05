package com.cyan.dataman.domain.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.domain.metadata.MetadataTable;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface MetadataTableAppConvert {

    MetadataTableAppConvert INSTANCE = Mappers.getMapper(MetadataTableAppConvert.class);

    MetadataTable toMetadataTable(MetadataTableCmd cmd);
}
