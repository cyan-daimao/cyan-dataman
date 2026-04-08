package com.cyan.dataman.domain.ds.repository;

import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.query.DsConfigFindQuery;
import com.cyan.dataman.domain.ds.query.DsConfigListQuery;

import java.util.List;

/**
 * 数据源配置仓储
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface DsConfigRepository {

    /**
     * 根据id获取数据源配置
     */
    DsConfig findById(String id);

    /**
     * 获取数据源配置列表
     */
    List<DsConfig> list(DsConfigListQuery query);

    /**
     * 查询数据源配置
     */
    DsConfig find(DsConfigFindQuery query);

    /**
     * 保存数据源配置
     */
    DsConfig save(DsConfig dsConfig);

    /**
     * 更新数据源配置
     */
    DsConfig update(DsConfig dsConfig);

    /**
     * 删除数据源配置
     */
    void deleteById(String id);

    /**
     * 根据名称获取数据源配置
     */
    DsConfig findByName(String ds);
}
