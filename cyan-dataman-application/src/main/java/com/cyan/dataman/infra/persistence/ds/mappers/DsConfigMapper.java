package com.cyan.dataman.infra.persistence.ds.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.ds.dos.DsConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源配置Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface DsConfigMapper extends BaseMapper<DsConfigDO> {
}
