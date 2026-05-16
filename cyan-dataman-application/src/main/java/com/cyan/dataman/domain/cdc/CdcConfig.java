package com.cyan.dataman.domain.cdc;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.enums.RunningStatus;
import com.cyan.dataman.enums.SyncTool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC 配置实体
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CdcConfig {

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
     * Debezium 连接器名称
     */
    private String connectorName;

    /**
     * Debezium server ID（唯一）
     */
    private Integer serverId;

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

    /**
     * 逻辑删除
     */
    private LocalDateTime deletedAt;

    /**
     * 验证
     */
    private void validate() {
        Assert.notBlank(this.name, new SilentException("CDC 配置名称不能为空"));
        Assert.notBlank(this.dsName, new SilentException("数据源名称不能为空"));
        Assert.notBlank(this.dbName, new SilentException("数据库名不能为空"));
        Assert.notBlank(this.tableName, new SilentException("表名不能为空"));
        Assert.notNull(this.syncTool, new SilentException("同步工具不能为空"));
        // Spark 模式需校验目标表名；Flink 模式自动生成 ODS 表名
        if (this.syncTool != SyncTool.FLINK) {
            Assert.notBlank(this.icebergTableName, new SilentException("目标 Iceberg 表名不能为空"));
        }
    }

    /**
     * 保存
     */
    public CdcConfig save(CdcConfigRepository repository) {
        autoGenerateIcebergTableName();
        validate();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = false;
        }
        this.msg = StrUtils.isBlank(this.msg)? "" : this.msg;
        return repository.save(this);
    }

    /**
     * 更新
     */
    public CdcConfig update(CdcConfigRepository repository) {
        autoGenerateIcebergTableName();
        validate();
        this.updatedAt = LocalDateTime.now();
        return repository.update(this);
    }

    /**
     * Flink 模式下自动生成 ODS 表名
     */
    private void autoGenerateIcebergTableName() {
        if (this.syncTool == SyncTool.FLINK && StrUtils.isBlank(this.icebergTableName)
                && StrUtils.isNotBlank(this.subjectCode) && StrUtils.isNotBlank(this.dsName)) {
            String safeSubject = this.subjectCode.replaceAll("[^a-zA-Z0-9_]", "_");
            String safeDs = this.dsName.replaceAll("[^a-zA-Z0-9_]", "_");
            this.icebergTableName = "ods_cdc_raw_" + safeSubject + "_" + safeDs;
        }
    }

    /**
     * 删除
     */
    public void delete(CdcConfigRepository repository) {
        repository.deleteById(this.id);
    }

    /**
     * 开启/关闭
     */
    public void toggle(CdcConfigRepository repository, Boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
        repository.update(this);
    }
}
