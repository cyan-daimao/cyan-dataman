package com.cyan.dataman.application.datasource.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据源-表bo对象
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DatasourceTableBO {

    /**
     * 库名
     */
    private String db;

    /**
     * 表名
     */
    private String name;
}
