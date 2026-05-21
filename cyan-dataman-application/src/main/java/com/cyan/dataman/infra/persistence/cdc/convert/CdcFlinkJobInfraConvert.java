package com.cyan.dataman.infra.persistence.cdc.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.cdc.CdcFlinkJob;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcFlinkJobDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * CDC Flink 作业配置转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface CdcFlinkJobInfraConvert {

    CdcFlinkJobInfraConvert INSTANCE = Mappers.getMapper(CdcFlinkJobInfraConvert.class);

    CdcFlinkJobDO toDO(CdcFlinkJob job);

    CdcFlinkJob toDomain(CdcFlinkJobDO dos);
}
