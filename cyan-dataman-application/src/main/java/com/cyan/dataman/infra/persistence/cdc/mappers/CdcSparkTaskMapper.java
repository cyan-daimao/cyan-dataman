package com.cyan.dataman.infra.persistence.cdc.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcSparkTaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CDC Spark 任务实例 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface CdcSparkTaskMapper extends BaseMapper<CdcSparkTaskDO> {
}
