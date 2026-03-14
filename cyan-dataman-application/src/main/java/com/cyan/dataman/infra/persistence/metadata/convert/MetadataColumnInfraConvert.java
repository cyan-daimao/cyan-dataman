package com.cyan.dataman.infra.persistence.metadata.convert;

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
@Mapper
public interface MetadataColumnInfraConvert {

    MetadataColumnInfraConvert INSTANCE = Mappers.getMapper(MetadataColumnInfraConvert.class);

    @Mapping(target = "col", source = "name")
    @Mapping(target = "dataType", expression = "java(com.cyan.dataman.enums.ColumnDataType.getByCode(columnValObj.getType().name()))")
    MetadataColumnDO toMetadataColumnDO(ColumnValObj columnValObj);

    List<MetadataColumnDO> toMetadataColumnDOList(List<ColumnValObj> columnValObjs);
}
