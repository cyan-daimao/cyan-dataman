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
 * 元数据血缘边数据对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("metadata_lineage_edge")
public class MetadataLineageEdgeDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 源节点唯一键
     */
    @TableField("source_key")
    private String sourceKey;

    /**
     * 目标节点唯一键
     */
    @TableField("target_key")
    private String targetKey;

    /**
     * 边类型
     */
    @TableField("edge_type")
    private String edgeType;

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
