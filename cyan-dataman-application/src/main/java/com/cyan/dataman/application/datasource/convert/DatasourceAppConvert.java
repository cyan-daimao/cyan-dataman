package com.cyan.dataman.application.datasource.convert;

import com.cyan.dataman.application.datasource.bo.DatasourceSchemaBO;
import com.cyan.dataman.application.datasource.bo.DatasourceTableBO;
import com.cyan.dataman.domain.datasource.DatasourceSchema;
import com.cyan.dataman.domain.datasource.DatasourceTable;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据源转化器
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface DatasourceAppConvert {

    DatasourceAppConvert INSTANCE = Mappers.getMapper(DatasourceAppConvert.class);

    DatasourceSchemaBO toDatasourceSchemaBO(DatasourceSchema datasourceSchema);

    DatasourceTableBO toDatasourceTableBO(DatasourceTable datasourceTable);
}
