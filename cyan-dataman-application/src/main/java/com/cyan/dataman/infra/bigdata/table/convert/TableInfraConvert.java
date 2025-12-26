package com.cyan.dataman.infra.bigdata.table.convert;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author cy.Y
 */
@Mapper
public interface TableInfraConvert {
    TableInfraConvert INSTANCE = Mappers.getMapper(TableInfraConvert.class);

}
