package com.cyan.dataman.infra.persistence.metadata.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.metadata.TableRelation;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableRelationDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 表关系基础设施层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface TableRelationInfraConvert {

    TableRelationInfraConvert INSTANCE = Mappers.getMapper(TableRelationInfraConvert.class);

    TableRelation toTableRelation(MetadataTableRelationDO relationDO);

    MetadataTableRelationDO toMetadataTableRelationDO(TableRelation relation);

    List<TableRelation> toTableRelationList(List<MetadataTableRelationDO> relationDOList);
}
