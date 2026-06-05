package com.cyan.dataman.domain.metadata.quality;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityAlertRepository;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 元数据质量告警领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityAlert {

    /**
     * 主键
     */
    private Long id;

    /**
     * 元数据表ID
     */
    private String tableId;

    /**
     * 运行ID
     */
    private Long runId;

    /**
     * 结果ID
     */
    private Long resultId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 告警标题
     */
    private String title;

    /**
     * 告警内容
     */
    private String message;

    /**
     * 严重等级
     */
    private String severity;

    /**
     * 告警状态
     */
    private String status;

    /**
     * 关闭人
     */
    private String closedBy;

    /**
     * 关闭时间
     */
    private LocalDateTime closedAt;

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
     * 保存告警
     */
    public MetadataQualityAlert save(MetadataQualityAlertRepository repository) {
        validate();
        this.status = "OPEN";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return repository.save(this);
    }

    /**
     * 关闭告警
     */
    public MetadataQualityAlert close(MetadataQualityAlertRepository repository, String operator) {
        this.status = "CLOSED";
        this.closedBy = operator;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return repository.update(this);
    }

    /**
     * 校验告警
     */
    private void validate() {
        Assert.notBlank(this.tableId, new SilentException("元数据表ID不能为空"));
        Assert.notNull(this.runId, new SilentException("运行ID不能为空"));
        Assert.notNull(this.resultId, new SilentException("结果ID不能为空"));
        Assert.notNull(this.ruleId, new SilentException("规则ID不能为空"));
        Assert.notBlank(this.title, new SilentException("告警标题不能为空"));
        Assert.notBlank(this.severity, new SilentException("严重等级不能为空"));
    }
}
