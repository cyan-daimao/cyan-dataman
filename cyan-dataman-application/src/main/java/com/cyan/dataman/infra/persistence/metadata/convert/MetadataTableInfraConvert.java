package com.cyan.dataman.infra.persistence.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataColumnDO;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface MetadataTableInfraConvert {

    MetadataTableInfraConvert INSTANCE = Mappers.getMapper(MetadataTableInfraConvert.class);

    @Mapping(target = "table.catalog",source = "dataCatalog")
    @Mapping(target = "table.schema",source = "dataSchema")
    @Mapping(target = "table.name",source = "tbl")
    @Mapping(target = "table.comment",source = "comment")
    @Mapping(target = "name",source = "tbl")
    MetadataTable toMetadataTable(MetadataTableDO metadataTableDO);

    @Mapping(source = "layerCode",target = "dataSchema")
    @Mapping(source = "name",target = "tbl")
    MetadataTableDO toMetadataTableDO(MetadataTable table);

    List<ColumnValObj> toMetadataColumns(List<MetadataColumnDO> metadataColumnDOS);

    @Mapping(source = "col",target = "name")
    @Mapping(source = "dataType",target = "type")
    ColumnValObj toMetadataColumn(MetadataColumnDO metadataColumnDO);

}
