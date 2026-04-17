package com.cyan.dataman.domain.cdc;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.domain.cdc.repository.CdcSparkJobRepository;
import com.cyan.dataman.enums.SyncMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC Spark 任务配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CdcSparkJob {

    /**
     * 主键
     */
    private String id;

    /**
     * CDC 配置 ID
     */
    private String cdcConfigId;

    /**
     * 同步模式（覆盖/追加）
     */
    private SyncMode syncMode;

    /**
     * 调度表达式 (Cron)
     */
    private String cronExpression;

    /**
     * 是否启用调度
     */
    private Boolean enabled;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    private LocalDateTime deletedAt;

    /**
     * 验证
     */
    private void validate() {
        Assert.notBlank(this.cdcConfigId, new SilentException("CDC 配置 ID 不能为空"));
        Assert.notNull(this.syncMode, new SilentException("同步模式不能为空"));
    }

    /**
     * 保存
     */
    public CdcSparkJob save(CdcSparkJobRepository repository) {
        validate();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = false;
        }
        return repository.save(this);
    }

    /**
     * 更新
     */
    public CdcSparkJob update(CdcSparkJobRepository repository) {
        validate();
        this.updatedAt = LocalDateTime.now();
        return repository.update(this);
    }

    /**
     * 删除
     */
    public void delete(CdcSparkJobRepository repository) {
        repository.deleteById(this.id);
    }
}
