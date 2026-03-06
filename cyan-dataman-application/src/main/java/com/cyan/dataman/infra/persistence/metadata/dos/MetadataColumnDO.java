package com.cyan.dataman.infra.persistence.metadata.dos;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataman.enums.ColumnDataType;
import com.cyan.dataman.enums.HeatLevel;
import com.cyan.dataman.enums.OnlineStatus;
import com.cyan.dataman.enums.SecretLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * 元数据表字段
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("metadata_column")
public class MetadataColumnDO {

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;


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
     * 字段名
     */
    @TableField(value = "col")
    private String col;

    /**
     * 数据类型
     */
    @TableField(value = "data_type")
    private ColumnDataType dataType;

    /**
     * 字段注释
     */
    @TableField(value = "comment")
    private String comment;

    /**
     * 可空
     */
    @TableField(value = "nullable")
    private Boolean nullable;

    /**
     * 秘密等级
     */
    @TableField(value = "secret_level")
    private SecretLevel secretLevel;

    /**
     * 默认值
     */
    @TableField(value = "default_value")
    private String defaultValue;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    /**
     * 删除时间
     */
    @TableField(value = "deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
