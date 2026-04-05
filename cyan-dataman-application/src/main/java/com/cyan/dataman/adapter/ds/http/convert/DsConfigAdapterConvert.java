package com.cyan.dataman.adapter.ds.http.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.adapter.ds.http.dto.ColumnDTO;
import com.cyan.dataman.adapter.ds.http.dto.DatabaseDTO;
import com.cyan.dataman.adapter.ds.http.dto.DsConfigDTO;
import com.cyan.dataman.adapter.ds.http.dto.IndexDTO;
import com.cyan.dataman.adapter.ds.http.dto.TableSchemaDTO;
import com.cyan.dataman.application.ds.bo.DsConfigBO;
import com.cyan.dataman.domain.ds.valobj.ColumnValObj;
import com.cyan.dataman.domain.ds.valobj.DatabaseValObj;
import com.cyan.dataman.domain.ds.valobj.IndexValObj;
import com.cyan.dataman.domain.ds.valobj.MysqlColumnValObj;
import com.cyan.dataman.domain.ds.valobj.PgsqlColumnValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.enums.DatasourceType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

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

    ColumnDTO toColumnDTO(ColumnValObj valObj);

    IndexDTO toIndexDTO(IndexValObj valObj);

    List<ColumnDTO> toColumnDTOList(List<ColumnValObj> valObjs);

    List<IndexDTO> toIndexDTOList(List<IndexValObj> valObjs);

    // ==================== DTO -> ColumnValObj 转换 ====================

    /**
     * 将 ColumnDTO 转换为 MysqlColumnValObj
     */
    @Mapping(target = "databaseType", constant = "MYSQL")
    MysqlColumnValObj toMysqlColumnValObj(ColumnDTO dto);

    /**
     * 将 ColumnDTO 转换为 PgsqlColumnValObj
     */
    @Mapping(target = "databaseType", constant = "POSTGRESQL")
    PgsqlColumnValObj toPgsqlColumnValObj(ColumnDTO dto);

    /**
     * 根据数据源类型批量转换 ColumnDTO 为 ColumnValObj
     */
    default List<ColumnValObj> toColumnValObjList(List<ColumnDTO> dtos, DatasourceType dsType) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(dto -> toColumnValObj(dto, dsType))
                .toList();
    }

    /**
     * 根据数据源类型转换 ColumnDTO 为 ColumnValObj
     */
    default ColumnValObj toColumnValObj(ColumnDTO dto, DatasourceType dsType) {
        if (dto == null) {
            return null;
        }
        if (dsType == DatasourceType.MYSQL) {
            return toMysqlColumnValObj(dto);
        } else if (dsType == DatasourceType.POSTGRESQL) {
            return toPgsqlColumnValObj(dto);
        }
        throw new IllegalArgumentException("不支持的数据源类型: " + dsType);
    }

    /**
     * 将 IndexDTO 转换为 IndexValObj
     */
    IndexValObj toIndexValObj(IndexDTO dto);

    /**
     * 批量转换 IndexDTO 为 IndexValObj
     */
    List<IndexValObj> toIndexValObjList(List<IndexDTO> dtos);
}
