package com.cyan.dataman.application.cdc.service;

/**
 * CDC Flink 同步服务
 * <p>
 * 管理 Flink CDC 作业的生命周期，包括创建、启动、停止、删除等操作。
 * 使用共享 Slot 机制实现多个表的同步，支持动态启用/禁用单个表的同步。
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface CdcFlinkSyncService {

    /**
     * 启动 Flink CDC 同步作业
     * <p>
     * 根据所有已启用的 FLINK 类型的 CdcConfig 创建 Flink 作业，
     * 从 Debezium Kafka 读取数据并写入 Iceberg 表。
     */
    void startFlinkSyncJob();

    /**
     * 停止 Flink CDC 同步作业
     * <p>
     * 停止所有正在运行的 Flink CDC 作业。
     */
    void stopFlinkSyncJob();

    /**
     * 为指定 CDC 配置启用 Flink 同步
     * <p>
     * 如果 Flink 作业尚未启动，则启动作业；
     * 如果作业已启动，则动态添加该表的同步任务。
     *
     * @param cdcConfigId CDC 配置 ID
     */
    void enableCdcSync(String cdcConfigId);

    /**
     * 为指定 CDC 配置禁用 Flink 同步
     * <p>
     * 动态停止该表的同步任务，但不影响其他表的同步。
     *
     * @param cdcConfigId CDC 配置 ID
     */
    void disableCdcSync(String cdcConfigId);

    /**
     * 停止指定的 Flink CDC 作业
     *
     * @param flinkJobId Flink 作业 ID
     */
    void cancelFlinkJob(String flinkJobId);

    /**
     * 刷新同步状态
     * <p>
     * 检查所有 CdcConfig 的 enabled 状态，更新对应的 Flink 作业。
     */
    void refreshSyncStatus();
}
