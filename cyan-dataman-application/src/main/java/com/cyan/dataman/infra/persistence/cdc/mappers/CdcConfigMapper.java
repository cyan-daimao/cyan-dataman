package com.cyan.dataman.infra.persistence.cdc.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CDC 配置 Mapper
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface CdcConfigMapper extends BaseMapper<CdcConfigDO> {

    /**
     * 获取下一个可用的 serverId
     */
    @Select("SELECT IFNULL(MAX(server_id) + 1, 30000) AS new_server_id FROM cdc_config")
    int findNextServerId();

    /**
     * 按数据源查询所有 CDC 配置（通过 dsId 关联）
     */
    @Select("SELECT * FROM cdc_config WHERE ds_id = #{dsId} AND deleted_at IS NULL")
    List<CdcConfigDO> findByDatasource(String dsId);

    /**
     * 按数据源查询已启用的 CDC 配置
     */
    @Select("SELECT * FROM cdc_config WHERE ds_id = #{dsId} AND enabled = true AND deleted_at IS NULL")
    List<CdcConfigDO> findEnabledByDatasource(String dsId);
}
