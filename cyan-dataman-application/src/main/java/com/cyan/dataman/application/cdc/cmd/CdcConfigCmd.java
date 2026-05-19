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
     * 数据源名称
     */
    @NotBlank(message = "数据源名称不能为空")
    private String dsName;

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
     * 主题编码（ODS 表前缀）
     */
    @NotBlank(message = "主题编码不能为空")
    private String subjectCode;

    /**
     * 目标 Iceberg 表名（方案 B 废弃，保留兼容）
     */
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
     * 密级（L1/L2/L3/L4）
     */
    private String secretLevel;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;
}
