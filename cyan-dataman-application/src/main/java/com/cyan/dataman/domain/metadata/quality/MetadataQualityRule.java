package com.cyan.dataman.domain.metadata.quality;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityRuleRepository;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据质量规则领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityRule {

    /**
     * 主键
     */
    private Long id;

    /**
     * 元数据表ID
     */
    private String tableId;

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
     * 规则配置JSON
     */
    private String configJson;

    /**
     * 过滤条件SQL
     */
    private String filterSql;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 是否启用
     */
    private Boolean enabled;

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
     * 保存规则
     */
    public MetadataQualityRule save(MetadataQualityRuleRepository repository) {
        validate();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = true;
        }
        return repository.save(this);
    }

    /**
     * 更新规则
     */
    public MetadataQualityRule update(MetadataQualityRuleRepository repository) {
        validate();
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = true;
        }
        return repository.update(this);
    }

    /**
     * 删除规则
     */
    public void delete(MetadataQualityRuleRepository repository) {
        repository.deleteById(this.id);
    }

    /**
     * 校验规则配置
     */
    private void validate() {
        Assert.notBlank(this.tableId, new SilentException("元数据表ID不能为空"));
        Assert.notBlank(this.ruleName, new SilentException("规则名称不能为空"));
        Assert.notBlank(this.ruleType, new SilentException("规则类型不能为空"));
        Assert.notBlank(this.dimension, new SilentException("质量维度不能为空"));
        Assert.notBlank(this.severity, new SilentException("严重等级不能为空"));
    }
}
