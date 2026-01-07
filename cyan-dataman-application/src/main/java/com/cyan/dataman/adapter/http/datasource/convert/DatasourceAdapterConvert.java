package com.cyan.dataman.adapter.http.datasource.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.http.datasource.dto.DatasourceSchemaDTO;
import com.cyan.dataman.adapter.http.datasource.dto.DatasourceTableDTO;
import com.cyan.dataman.application.datasource.bo.DatasourceSchemaBO;
import com.cyan.dataman.application.datasource.bo.DatasourceTableBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据源转化器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface DatasourceAdapterConvert {

    DatasourceAdapterConvert INSTANCE = Mappers.getMapper(DatasourceAdapterConvert.class);

    DatasourceSchemaDTO toDatasourceSchemaDTO(DatasourceSchemaBO bo);

    DatasourceTableDTO toDatasourceTableDTO(DatasourceTableBO datasourceTableBO);
}
