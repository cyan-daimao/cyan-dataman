package com.cyan.dataman.application.cdc.convert;

import com.cyan.arch.base.mapstruct.MapstructConvert;
import com.cyan.dataman.application.cdc.bo.CdcConfigBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkJobBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkTaskBO;
import com.cyan.dataman.application.cdc.cmd.CdcConfigCmd;
import com.cyan.dataman.application.cdc.cmd.CdcSparkJobCmd;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.domain.cdc.CdcSparkTask;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * CDC 应用层转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public interface CdcAppConvert {

    CdcAppConvert INSTANCE = Mappers.getMapper(CdcAppConvert.class);

    // CdcConfig
    CdcConfigBO toBO(CdcConfig config);

    CdcConfig toDomain(CdcConfigCmd cmd);

    // CdcSparkJob
    CdcSparkJobBO toBO(CdcSparkJob job);

    CdcSparkJob toDomain(CdcSparkJobCmd cmd);

    // CdcSparkTask
    CdcSparkTaskBO toBO(CdcSparkTask task);
}
