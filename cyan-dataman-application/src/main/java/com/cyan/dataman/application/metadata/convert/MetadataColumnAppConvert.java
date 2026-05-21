package com.cyan.dataman.application.metadata.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.application.metadata.bo.MetadataColumnBO;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 元数据字段应用层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface MetadataColumnAppConvert {
    MetadataColumnAppConvert INSTANCE = Mappers.getMapper(MetadataColumnAppConvert.class);

    @Mapping(source = "name", target = "col")
    @Mapping(source = "type", target = "dataType")
    MetadataColumnBO toMetadataColumnBO(ColumnValObj columnValObj);

    List<MetadataColumnBO> toMetadataColumnBOList(List<ColumnValObj> columnValObjs);
}
