package com.cyan.dataman.adapter.ds.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.ds.http.dto.DatabaseDTO;
import com.cyan.dataman.adapter.ds.http.dto.DsConfigDTO;
import com.cyan.dataman.adapter.ds.http.dto.TableSchemaDTO;
import com.cyan.dataman.application.ds.bo.DsConfigBO;
import com.cyan.dataman.domain.ds.valobj.DatabaseValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据源配置适配层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(uses = MapstructConvert.class)
public interface DsConfigAdapterConvert {
    DsConfigAdapterConvert INSTANCE = Mappers.getMapper(DsConfigAdapterConvert.class);

    DsConfigDTO toDsConfigDTO(DsConfigBO bo);

    DatabaseDTO toDatabaseDTO(DatabaseValObj valObj);

    TableSchemaDTO toTableSchemaDTO(TableSchemaValObj valObj);
}
