package com.cyan.dataman.infra.persistence.metadata.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cyan.dataman.enums.PartitionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据表分区
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@TableName("metadata_partition")
public class MetadataPartitionDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 目录
     */
    @TableField(value = "data_catalog")
    private String dataCatalog;

    /**
     * 库
     */
    @TableField(value = "data_schema")
    private String dataSchema;

    /**
     * 表名
     */
    @TableField(value = "tbl")
    private String tbl;

    /**
     * 分区字段
     */
    @TableField(value = "col")
    private String col;

    /**
     * 分区类型
     */
    @TableField(value = "partition_type")
    private PartitionType partitionType;

    /**
     * 分区参数
     */
    @TableField(value = "param")
    private Integer param;

    /**
     * 分区顺序
     */
    @TableField(value = "sort_order")
    private Integer sortOrder;

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
