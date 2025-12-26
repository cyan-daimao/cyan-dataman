package com.cyan.dataman.domain.datasource.repository;

import com.cyan.dataman.domain.datasource.DatasourceTable;
import com.cyan.dataman.enums.StorageType;

import java.util.List;

/**
 * 数据源仓储
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface DatasourceRepository {

    /**
     * 获取数据源表列表
     *
     * @return 数据源表列表
     */
    List<DatasourceTable> list();

    /**
     * 获取数据源类型
     *
     * @return 数据源类型
     */
    StorageType getStorageType();
}
