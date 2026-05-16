package com.cyan.dataman.domain.cdc.query;

import com.cyan.dataman.enums.SyncTool;
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
     * 主题编码
     */
    private String subjectCode;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 同步工具
     */
    private SyncTool syncTool;
}
