package com.cyan.dataman.adapter.metadata.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.metadata.http.dto.SchemaDTO;
import com.cyan.dataman.domain.metadata.valobj.SchemaValObj;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 库适配器层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface SchemaAdapterConvert {

    SchemaAdapterConvert INSTANCE = Mappers.getMapper(SchemaAdapterConvert.class);

    SchemaDTO toSchemaDTO(SchemaValObj valObj);

    List<SchemaDTO> toSchemaDTOList(List<SchemaValObj> valObjs);
}
