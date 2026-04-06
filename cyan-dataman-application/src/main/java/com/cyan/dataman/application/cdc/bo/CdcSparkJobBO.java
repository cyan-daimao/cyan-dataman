package com.cyan.dataman.application.cdc.bo;

import com.cyan.dataman.enums.SyncMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * CDC Spark 作业配置业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CdcSparkJobBO {

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
     * Spark SQL 模板
     */
    private String sparkSql;

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
}
