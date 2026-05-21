package com.cyan.dataman.application.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.client.table.dto.TableRelationDTO;
import com.cyan.dataman.domain.metadata.TableRelation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 表关系应用层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface TableRelationAppConvert {

    TableRelationAppConvert INSTANCE = Mappers.getMapper(TableRelationAppConvert.class);

    @Mapping(target = "id", expression = "java(relation.getId() == null ? null : relation.getId().toString())")
    @Mapping(target = "sourceTableComment", ignore = true)
    @Mapping(target = "targetTableComment", ignore = true)
    TableRelationDTO toTableRelationDTO(TableRelation relation);

    List<TableRelationDTO> toTableRelationDTOList(List<TableRelation> relations);
}
