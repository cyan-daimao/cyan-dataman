package com.cyan.dataman.application.cdc.bo;

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
