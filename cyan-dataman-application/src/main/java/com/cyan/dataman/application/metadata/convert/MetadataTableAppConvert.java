package com.cyan.dataman.application.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.domain.metadata.MetadataTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 元数据转换
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataTableAppConvert {
    MetadataTableAppConvert INSTANCE = Mappers.getMapper(MetadataTableAppConvert.class);

    MetadataTableBO toMetadataTableBO(MetadataTable metadataTable);

    @Mapping(target = "layerCode", expression = "java(cmd.getLayerCode().getCode())")
    @Mapping(target = "table", source = "tableValObj")
    MetadataTable toMetadataTable(MetadataTableCmd cmd);
}
