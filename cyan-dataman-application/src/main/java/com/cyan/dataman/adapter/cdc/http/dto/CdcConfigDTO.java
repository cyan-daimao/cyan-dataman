package com.cyan.dataman.adapter.cdc.http.dto;

import com.cyan.dataman.enums.RunningStatus;
import com.cyan.dataman.enums.SyncTool;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC 配置 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class CdcConfigDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * CDC 配置名称
     */
    private String name;

    /**
     * 数据源 ID
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
     * 目标 Iceberg 表名
     */
    private String icebergTableName;

    /**
     * 同步工具
     */
    private SyncTool syncTool;

    /**
     * 同步 SQL
     */
    private String syncSql;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 描述
     */
    private String description;

    /**
     * Debezium 连接器名称
     */
    private String connectorName;

    /**
     * 连接器运行状态
     */
    private RunningStatus runningStatus;

    /**
     * 状态消息
     */
    private String msg;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
