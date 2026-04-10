package com.cyan.dataman.domain.cdc.query;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * CDC 配置列表查询
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class CdcConfigListQuery {

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据库名
     */
    private String dbName;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
