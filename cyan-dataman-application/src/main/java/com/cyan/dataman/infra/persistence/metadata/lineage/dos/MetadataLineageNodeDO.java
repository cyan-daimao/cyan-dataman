package com.cyan.dataman.infra.persistence.metadata.lineage.dos;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 元数据血缘节点数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("metadata_lineage_node")
public class MetadataLineageNodeDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 节点唯一键
     */
    @TableField("node_key")
    private String nodeKey;

    /**
     * 节点类型
     */
    @TableField("node_type")
    private String nodeType;

    /**
     * 节点名称
     */
    @TableField("node_name")
    private String nodeName;

    /**
     * 来源服务名
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 来源业务ID
     */
    @TableField("ref_id")
    private String refId;

    /**
     * 表唯一引用
     */
    @TableField("table_ref")
    private String tableRef;

    /**
     * 字段名
     */
    @TableField("column_name")
    private String columnName;

    /**
     * 扩展属性 JSON
     */
    @TableField("properties_json")
    private String propertiesJson;

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
     * 删除时间
     */
    @TableField("deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
