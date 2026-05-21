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
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface DsConfigAdapterConvert {
    DsConfigAdapterConvert INSTANCE = Mappers.getMapper(DsConfigAdapterConvert.class);

    DsConfigDTO toDsConfigDTO(DsConfigBO bo);

    DatabaseDTO toDatabaseDTO(DatabaseValObj valObj);

    TableSchemaDTO toTableSchemaDTO(TableSchemaValObj valObj);

    IndexDTO toIndexDTO(IndexValObj valObj);

    List<IndexDTO> toIndexDTOList(List<IndexValObj> valObjs);

    // ==================== ColumnValObj -> ColumnDTO 转换 ====================

    /**
     * 将 ColumnValObj 转换为 ColumnDTO（支持多态）
     */
    default ColumnDTO toColumnDTO(ColumnValObj valObj) {
        if (valObj == null) {
            return null;
        }
        ColumnDTO dto = new ColumnDTO()
                .setName(valObj.getName())
                .setType(valObj.getType())
                .setComment(valObj.getComment())
                .setNullable(valObj.getNullable())
                .setAutoIncrement(valObj.getAutoIncrement())
                .setDefaultValue(valObj.getDefaultValue())
                .setPrecision(valObj.getPrecision())
                .setScale(valObj.getScale());
        
        // 处理 MySQL 特有字段
        if (valObj instanceof MysqlColumnValObj mysqlCol) {
            dto.setUnsigned(mysqlCol.getUnsigned())
               .setZerofill(mysqlCol.getZerofill())
               .setCharset(mysqlCol.getCharset())
               .setCollation(mysqlCol.getCollation());
        }
        // 处理 PostgreSQL 特有字段
        else if (valObj instanceof PgsqlColumnValObj pgsqlCol) {
            dto.setArrayDimensions(pgsqlCol.getArrayDimensions())
               .setWithTimeZone(pgsqlCol.getWithTimeZone());
        }
        
        return dto;
    }

    /**
     * 批量转换 ColumnValObj 为 ColumnDTO
     */
    default List<ColumnDTO> toColumnDTOList(List<ColumnValObj> valObjs) {
        if (valObjs == null) {
            return null;
        }
        return valObjs.stream()
                .map(this::toColumnDTO)
                .toList();
    }

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
