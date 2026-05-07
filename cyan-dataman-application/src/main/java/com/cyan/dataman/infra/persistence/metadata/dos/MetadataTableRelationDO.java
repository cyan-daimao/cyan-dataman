package com.cyan.dataman.infra.persistence.metadata.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据表关系
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("metadata_table_relation")
public class MetadataTableRelationDO {

    /**
     * 主键
     */
    @TableId("id")
    private Long id;

    /**
     * 源表 catalog
     */
    @TableField("source_catalog")
    private String sourceCatalog;

    /**
     * 源表 schema
     */
    @TableField("source_schema")
    private String sourceSchema;

    /**
     * 源表名
     */
    @TableField("source_table")
    private String sourceTable;

    /**
     * 源表字段
     */
    @TableField("source_column")
    private String sourceColumn;

    /**
     * 目标表 catalog
     */
    @TableField("target_catalog")
    private String targetCatalog;

    /**
     * 目标表 schema
     */
    @TableField("target_schema")
    private String targetSchema;

    /**
     * 目标表名
     */
    @TableField("target_table")
    private String targetTable;

    /**
     * 目标表字段
     */
    @TableField("target_column")
    private String targetColumn;

    /**
     * JOIN类型：LEFT/INNER/RIGHT
     */
    @TableField("join_type")
    private String joinType;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建人
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 创建人
     */
    @TableField("updated_by")
    private String updatedBy;

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
}
