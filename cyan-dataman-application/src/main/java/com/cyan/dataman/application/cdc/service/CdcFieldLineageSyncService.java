package com.cyan.dataman.application.cdc.service;

import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcSparkJob;

/**
 * CDC 字段血缘同步服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface CdcFieldLineageSyncService {

    /**
     * 同步 Flink CDC 配置字段血缘
     *
     * @param config CDC 配置
     */
    void syncFlinkConfig(CdcConfig config);

    /**
     * 清理 Flink CDC 配置字段血缘
     *
     * @param cdcConfigId CDC 配置 ID
     */
    void clearFlinkConfig(String cdcConfigId);

    /**
     * 同步 Spark CDC 作业字段血缘
     *
     * @param job    Spark 作业
     * @param config CDC 配置
     */
    void syncSparkJob(CdcSparkJob job, CdcConfig config);

    /**
     * 清理 Spark CDC 作业字段血缘
     *
     * @param sparkJobId Spark 作业 ID
     */
    void clearSparkJob(String sparkJobId);
}
