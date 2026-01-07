package com.cyan.dataman.application.datasource;

import com.cyan.dataman.application.datasource.bo.DatasourceSchemaBO;
import com.cyan.dataman.application.datasource.bo.DatasourceTableBO;
import com.cyan.dataman.domain.datasource.query.DatasourceTableQuery;

import java.util.List;

/**
 * 数据源服务
 * @author cy.Y
 * @since 1.0.0
 */
public interface DatasourceService {

    /**
     * 获取数据源-库
     */
    List<DatasourceSchemaBO> listDB(DatasourceTableQuery query);


    /**
     * 获取数据源-表
     */
    List<DatasourceTableBO> listTable(DatasourceTableQuery query);
}
