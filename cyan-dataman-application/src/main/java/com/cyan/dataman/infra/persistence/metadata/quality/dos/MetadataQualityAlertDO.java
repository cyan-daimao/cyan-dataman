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
 * 元数据质量告警DO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("metadata_quality_alert")
public class MetadataQualityAlertDO {

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
     * 运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 结果ID
     */
    @TableField("result_id")
    private Long resultId;

    /**
     * 规则ID
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 告警标题
     */
    @TableField("title")
    private String title;

    /**
     * 告警内容
     */
    @TableField("message")
    private String message;

    /**
     * 严重等级
     */
    @TableField("severity")
    private String severity;

    /**
     * 告警状态
     */
    @TableField("status")
    private String status;

    /**
     * 关闭人
     */
    @TableField("closed_by")
    private String closedBy;

    /**
     * 关闭时间
     */
    @TableField("closed_at")
    private LocalDateTime closedAt;

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
