package com.cyan.dataman.application.cdc.cmd;

import com.cyan.dataman.enums.SyncMode;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * CDC Spark 作业配置命令对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class CdcSparkJobCmd {

    /**
     * CDC 配置 ID
     */
    private String cdcConfigId;

    /**
     * 同步模式
     */
    private SyncMode syncMode;

    /**
     * 调度表达式 (Cron)
     */
    private String cronExpression;

    /**
     * 是否启用
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
}
