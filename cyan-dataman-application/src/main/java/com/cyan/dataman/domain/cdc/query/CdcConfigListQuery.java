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
     * 数据源名称（API 层使用）
     */
    private String dsName;

    /**
     * 数据源 ID（内部查询使用，由服务层从 dsName 解析）
     */
    private String dsId;

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
