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
 * 元数据质量结果DO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("metadata_quality_result")
public class MetadataQualityResultDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 运行ID
     */
    @TableField("run_id")
    private Long runId;

    /**
     * 规则ID
     */
    @TableField("rule_id")
    private Long ruleId;

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
     * 检查状态
     */
    @TableField("status")
    private String status;

    /**
     * 实际值
     */
    @TableField("actual_value")
    private String actualValue;

    /**
     * 期望值
     */
    @TableField("expected_value")
    private String expectedValue;

    /**
     * 总行数
     */
    @TableField("total_count")
    private Long totalCount;

    /**
     * 失败行数
     */
    @TableField("fail_count")
    private Long failCount;

    /**
     * 样本SQL
     */
    @TableField("sample_sql")
    private String sampleSql;

    /**
     * 明细JSON
     */
    @TableField("detail_json")
    private String detailJson;

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
