package com.cyan.dataman.infra.persistence.cdc.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataman.enums.RunningStatus;
import com.cyan.dataman.enums.SyncTool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC 配置表
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("cdc_config")
public class CdcConfigDO {

    /**
     * 主键
     */
    @TableId("id")
    private String id;

    /**
     * CDC 配置名称（唯一标识）
     */
    @TableField("name")
    private String name;

    /**
     * 数据源名称
     */
    @TableField("ds_name")
    private String dsName;

    /**
     * 数据库名
     */
    @TableField("db_name")
    private String dbName;

    /**
     * 表名
     */
    @TableField("table_name")
    private String tableName;

    /**
     * 目标 Iceberg 表名
     */
    @TableField("iceberg_table_name")
    private String icebergTableName;

    /**
     * 同步工具
     */
    @TableField("sync_tool")
    private SyncTool syncTool;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 同步 SQL
     */
    @TableField("sync_sql")
    private String syncSql;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * Debezium 连接器名称
     */
    @TableField("connector_name")
    private String connectorName;

    /**
     * Debezium server ID
     */
    @TableField("server_id")
    private Integer serverId;

    /**
     * 连接器运行状态
     */
    @TableField("running_status")
    private RunningStatus runningStatus;

    /**
     * 状态消息
     */
    @TableField("msg")
    private String msg;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 修改人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableField("deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
