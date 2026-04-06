package com.cyan.dataman.infra.persistence.cdc.convert;

import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * CDC 配置转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface CdcConfigInfraConvert {

    CdcConfigInfraConvert INSTANCE = Mappers.getMapper(CdcConfigInfraConvert.class);

    CdcConfigDO toDO(CdcConfig config);

    CdcConfig toDomain(CdcConfigDO dos);
}
