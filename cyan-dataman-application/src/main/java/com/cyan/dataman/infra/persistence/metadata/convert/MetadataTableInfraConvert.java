package com.cyan.dataman.infra.persistence.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataTableInfraConvert {

    MetadataTableInfraConvert INSTANCE = Mappers.getMapper(MetadataTableInfraConvert.class);

    @Mapping(target = "table.catalog", source = "dataCatalog")
    @Mapping(target = "table.schema", source = "dataSchema")
    @Mapping(target = "table.name", source = "tbl")
    @Mapping(target = "table.comment", source = "comment")
    @Mapping(target = "name", source = "tbl")
    MetadataTable toMetadataTable(MetadataTableDO metadataTableDO);

    @Mapping(source = "layerCode", target = "dataSchema")
    @Mapping(source = "name", target = "tbl")
    @Mapping(source = "table.catalog", target = "dataCatalog")
    MetadataTableDO toMetadataTableDO(MetadataTable table);

}
