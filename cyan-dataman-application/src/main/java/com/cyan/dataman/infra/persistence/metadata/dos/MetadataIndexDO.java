package com.cyan.dataman.infra.persistence.metadata.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * 元数据表字段索引
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("metadata_index")
public class MetadataIndexDO {

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;


    /**
     * 目录
     */
    @TableField(value = "catalog")
    private String name;

    /**
     * 目录
     */
    @TableField(value = "catalog")
    private String catalog;

    /**
     * 库
     */
    @TableField(value = "schema")
    private String schema;

    /**
     * 表名
     */
    @TableField(value = "tbl")
    private String tbl;

    /**
     * 索引类型
     */
    @TableField(value = "index_type")
    private String indexType;

    /**
     * 字段JSON_ARRAY
     */
    @TableField(value = "columns")
    private String columns;

    /**
     * 字段注释
     */
    @TableField(value = "comment")
    private String comment;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;
    /**
     * 修改时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    @TableField(value = "deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
