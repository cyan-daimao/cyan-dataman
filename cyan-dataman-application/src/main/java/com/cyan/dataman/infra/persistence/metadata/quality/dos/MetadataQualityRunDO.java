package com.cyan.dataman.infra.persistence.metadata.quality.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 元数据质量运行DO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("metadata_quality_run")
public class MetadataQualityRunDO {

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
     * 运行状态
     */
    @TableField("status")
    private String status;

    /**
     * 质量分数
     */
    @TableField("score")
    private BigDecimal score;

    /**
     * 通过规则数
     */
    @TableField("pass_count")
    private Integer passCount;

    /**
     * 警告规则数
     */
    @TableField("warn_count")
    private Integer warnCount;

    /**
     * 失败规则数
     */
    @TableField("fail_count")
    private Integer failCount;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    @TableField("ended_at")
    private LocalDateTime endedAt;

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
