package com.cyan.dataman.domain.datasource.repository;

import com.cyan.dataman.domain.datasource.DatasourceSchema;
import com.cyan.dataman.domain.datasource.DatasourceTable;
import com.cyan.dataman.domain.datasource.query.DatasourceTableQuery;
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
     * 获取数据源类型
     *
     * @return 数据源类型
     */
    StorageType getStorageType();

    /**
     * 获取数据源-表列表
     *
     */
    List<DatasourceTable> listTable(DatasourceTableQuery query);

    /**
     * 获取数据源-库
     */
    List<DatasourceSchema> listSchemas();
}
