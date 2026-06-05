package com.cyan.dataman.domain.metadata.quality;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityRunRepository;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 元数据质量运行领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataQualityRun {

    /**
     * 主键
     */
    private Long id;

    /**
     * 元数据表ID
     */
    private String tableId;

    /**
     * 运行状态
     */
    private String status;

    /**
     * 质量分数
     */
    private BigDecimal score;

    /**
     * 通过规则数
     */
    private Integer passCount;

    /**
     * 警告规则数
     */
    private Integer warnCount;

    /**
     * 失败规则数
     */
    private Integer failCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime endedAt;

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
     * 创建运行记录
     */
    public MetadataQualityRun save(MetadataQualityRunRepository repository) {
        Assert.notBlank(this.tableId, new SilentException("元数据表ID不能为空"));
        this.status = "RUNNING";
        this.startedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.passCount = 0;
        this.warnCount = 0;
        this.failCount = 0;
        return repository.save(this);
    }

    /**
     * 完成运行
     */
    public MetadataQualityRun finish(MetadataQualityRunRepository repository) {
        this.status = "SUCCESS";
        this.endedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return repository.update(this);
    }

    /**
     * 标记运行失败
     */
    public MetadataQualityRun fail(MetadataQualityRunRepository repository, String message) {
        this.status = "FAILED";
        this.errorMessage = message;
        this.endedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return repository.update(this);
    }
}
