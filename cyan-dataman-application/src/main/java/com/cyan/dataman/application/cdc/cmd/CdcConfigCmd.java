package com.cyan.dataman.application.cdc.cmd;

import com.cyan.dataman.enums.SyncTool;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * CDC 配置命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class CdcConfigCmd {

    /**
     * CDC 配置名称
     */
    @NotBlank(message = "CDC 配置名称不能为空")
    private String name;

    /**
     * 数据源 ID
     */
    @NotBlank(message = "数据源 ID 不能为空")
    private String dsId;

    /**
     * 数据库名
     */
    @NotBlank(message = "数据库名不能为空")
    private String dbName;

    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * 目标 Iceberg 表名
     */
    @NotBlank(message = "目标 Iceberg 表名不能为空")
    private String icebergTableName;

    /**
     * 同步工具
     */
    @NotNull(message = "同步工具不能为空")
    private SyncTool syncTool;

    /**
     * 同步 SQL
     */
    private String syncSql;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;
}
