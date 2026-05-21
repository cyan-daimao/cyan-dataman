package com.cyan.dataman.infra.persistence.metadata.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataColumnDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据字段转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataColumnInfraConvert {

    MetadataColumnInfraConvert INSTANCE = Mappers.getMapper(MetadataColumnInfraConvert.class);

    @Mapping(source = "col", target = "name")
    @Mapping(source = "dataType", target = "type")
    ColumnValObj toColumnValObj(MetadataColumnDO metadataColumnDO);

    List<ColumnValObj> toColumnValObjList(List<MetadataColumnDO> metadataColumnDOs);

    @Mapping(target = "col", source = "name")
    @Mapping(target = "dataType", expression = "java(columnValObj.getColumnDataType())")
    MetadataColumnDO toMetadataColumnDO(ColumnValObj columnValObj);

    List<MetadataColumnDO> toMetadataColumnDOList(List<ColumnValObj> columnValObjs);
}