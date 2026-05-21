package com.cyan.dataman.application.ds.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.dataman.application.ds.bo.DsConfigBO;
import com.cyan.dataman.application.ds.cmd.DsConfigCmd;
import com.cyan.dataman.domain.ds.DsConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 数据源配置应用层转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface DsConfigAppConvert {
    DsConfigAppConvert INSTANCE = Mappers.getMapper(DsConfigAppConvert.class);

    DsConfigBO toDsConfigBO(DsConfig dsConfig);

    DsConfig toDsConfig(DsConfigCmd cmd);
}
