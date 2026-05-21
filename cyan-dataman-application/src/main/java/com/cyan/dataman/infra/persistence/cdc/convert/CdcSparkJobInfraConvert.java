package com.cyan.dataman.infra.persistence.cdc.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcSparkJobDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * CDC Spark 作业配置转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface CdcSparkJobInfraConvert {

    CdcSparkJobInfraConvert INSTANCE = Mappers.getMapper(CdcSparkJobInfraConvert.class);

    CdcSparkJobDO toDO(CdcSparkJob job);

    CdcSparkJob toDomain(CdcSparkJobDO dos);
}
