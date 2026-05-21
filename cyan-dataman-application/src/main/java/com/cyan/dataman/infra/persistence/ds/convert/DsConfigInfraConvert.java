package com.cyan.dataman.infra.persistence.ds.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.infra.persistence.ds.dos.DsConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据源配置转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface DsConfigInfraConvert {
    DsConfigInfraConvert INSTANCE = Mappers.getMapper(DsConfigInfraConvert.class);

    DsConfig toDsConfig(DsConfigDO dsConfigDO);

    DsConfigDO toDsConfigDO(DsConfig dsConfig);
}
