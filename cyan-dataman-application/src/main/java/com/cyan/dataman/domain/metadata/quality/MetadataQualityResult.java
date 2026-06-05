package com.cyan.dataman.domain.metadata.quality;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据质量结果领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityResult {

    /**
     * 主键
     */
    private Long id;

    /**
     * 运行ID
     */
    private Long runId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 质量维度
     */
    private String dimension;

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 检查状态
     */
    private String status;

    /**
     * 实际值
     */
    private String actualValue;

    /**
     * 期望值
     */
    private String expectedValue;

    /**
     * 总行数
     */
    private Long totalCount;

    /**
     * 失败行数
     */
    private Long failCount;

    /**
     * 样本SQL
     */
    private String sampleSql;

    /**
     * 明细JSON
     */
    private String detailJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 校验结果
     */
    public void validate() {
        Assert.notNull(this.runId, new SilentException("运行ID不能为空"));
        Assert.notNull(this.ruleId, new SilentException("规则ID不能为空"));
        Assert.notBlank(this.ruleName, new SilentException("规则名称不能为空"));
        Assert.notBlank(this.ruleType, new SilentException("规则类型不能为空"));
        Assert.notBlank(this.dimension, new SilentException("质量维度不能为空"));
        Assert.notBlank(this.status, new SilentException("检查状态不能为空"));
    }
}
