package com.cyan.dataman.infra.persistence.cdc.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcSparkJobDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CDC Spark 作业配置 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface CdcSparkJobMapper extends BaseMapper<CdcSparkJobDO> {
}
