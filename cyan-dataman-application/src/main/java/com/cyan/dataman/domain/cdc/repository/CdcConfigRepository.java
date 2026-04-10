package com.cyan.dataman.domain.cdc.repository;

import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;

import java.util.List;

/**
 * CDC 配置仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface CdcConfigRepository {

    /**
     * 保存
     */
    CdcConfig save(CdcConfig config);

    /**
     * 更新
     */
    CdcConfig update(CdcConfig config);

    /**
     * 根据 ID 查找
     */
    CdcConfig findById(String id);

    /**
     * 根据名称查找
     */
    CdcConfig findByName(String name);

    /**
     * 查询列表
     */
    List<CdcConfig> list(CdcConfigListQuery query);

    /**
     * 删除
     */
    void deleteById(String id);

    /**
     * 按数据源查询所有 CDC 配置
     */
    List<CdcConfig> findByDatasource(String dsId);

    /**
     * 按数据源查询已启用的 CDC 配置
     */
    List<CdcConfig> findEnabledByDatasource(String dsId);

    /**
     * 获取下一个可用的 serverId
     */
    int findNextServerId();
}
