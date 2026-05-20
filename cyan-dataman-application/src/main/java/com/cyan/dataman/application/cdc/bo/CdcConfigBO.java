package com.cyan.dataman.application.cdc.bo;

import com.cyan.dataman.enums.RunningStatus;
import com.cyan.dataman.enums.SyncTool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC 配置业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CdcConfigBO {

    /**
     * 主键
     */
    private String id;

    /**
     * CDC 配置名称（唯一标识）
     */
    private String name;

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
     * 主题编码（ODS 表前缀）
     */
    private String subjectCode;

    /**
     * 目标 Iceberg 表名（方案 B 废弃，保留兼容）
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
     * 密级（L1/L2/L3/L4）
     */
    private String secretLevel;

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
