package com.cyan.dataman.domain.datasource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据源表信息
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DatasourceTable {

    /**
     * 库名
     */
    private String db;
    /**
     * 表名
     */
    private String name;
}
