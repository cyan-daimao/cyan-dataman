package com.cyan.dataman.domain.datasource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据库信息
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class DatasourceTable {

    /**
     * 数据库名称
     */
    private String db;

    /**
     * 表名
     */
    private String name;
}
