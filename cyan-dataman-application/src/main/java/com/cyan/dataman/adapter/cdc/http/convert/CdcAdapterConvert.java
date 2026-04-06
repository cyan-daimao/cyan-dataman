package com.cyan.dataman.adapter.cdc.http.convert;

import com.cyan.dataman.adapter.cdc.http.dto.CdcConfigDTO;
import com.cyan.dataman.adapter.cdc.http.dto.CdcSparkJobDTO;
import com.cyan.dataman.adapter.cdc.http.dto.CdcSparkTaskDTO;
import com.cyan.dataman.application.cdc.bo.CdcConfigBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkJobBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkTaskBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * CDC 适配器转换器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface CdcAdapterConvert {

    CdcAdapterConvert INSTANCE = Mappers.getMapper(CdcAdapterConvert.class);

    CdcConfigDTO toCdcConfigDTO(CdcConfigBO bo);

    CdcSparkJobDTO toCdcSparkJobDTO(CdcSparkJobBO bo);

    CdcSparkTaskDTO toCdcSparkTaskDTO(CdcSparkTaskBO bo);
}
