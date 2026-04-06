package com.cyan.dataman.infra.persistence.cdc.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CDC 配置 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface CdcConfigMapper extends BaseMapper<CdcConfigDO> {
}
