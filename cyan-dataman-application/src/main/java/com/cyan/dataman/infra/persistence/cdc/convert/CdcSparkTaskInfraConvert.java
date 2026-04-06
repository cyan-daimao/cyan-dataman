package com.cyan.dataman.infra.persistence.cdc.convert;

import com.cyan.dataman.domain.cdc.CdcSparkTask;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcSparkTaskDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * CDC Spark 任务实例转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface CdcSparkTaskInfraConvert {

    CdcSparkTaskInfraConvert INSTANCE = Mappers.getMapper(CdcSparkTaskInfraConvert.class);

    CdcSparkTaskDO toDO(CdcSparkTask task);

    CdcSparkTask toDomain(CdcSparkTaskDO dos);
}
