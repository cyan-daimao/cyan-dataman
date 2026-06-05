package com.cyan.dataman.infra.persistence.metadata.quality.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据质量规则DO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("metadata_quality_rule")
public class MetadataQualityRuleDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 元数据表ID
     */
    @TableField("table_id")
    private Long tableId;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 规则类型
     */
    @TableField("rule_type")
    private String ruleType;

    /**
     * 质量维度
     */
    @TableField("dimension")
    private String dimension;

    /**
     * 字段名
     */
    @TableField("column_name")
    private String columnName;

    /**
     * 规则配置JSON
     */
    @TableField("config_json")
    private String configJson;

    /**
     * 过滤条件SQL
     */
    @TableField("filter_sql")
    private String filterSql;

    /**
     * 严重等级
     */
    @TableField("severity")
    private String severity;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

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
