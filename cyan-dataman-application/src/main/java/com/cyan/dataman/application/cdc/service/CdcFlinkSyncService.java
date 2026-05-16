package com.cyan.dataman.application.cdc.service;

/**
 * CDC Flink 同步服务
 * <p>
 * 管理 Flink CDC 作业的生命周期，包括创建、启动、停止、删除等操作。
 * 采用 Flink SQL 方式提交作业，每个数据源对应一个 Flink SQL 作业，
 * 将 Debezium Kafka 数据写入统一的 ODS Iceberg 表。
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface CdcFlinkSyncService {

    /**
     * 启动 Flink CDC 同步作业
     * <p>
     * 根据所有已启用的 FLINK 类型的 CdcConfig，按数据源分组，
     * 每个数据源生成并提交一个 Flink SQL 作业。
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
     * 如果该数据源没有运行中的 Flink 作业，则生成 SQL 并提交新作业；
     * 已有作业时，Debezium Connector 会自动将新表数据发送到 Kafka，
     * Flink SQL 作业通过 topic pattern 自动消费。
     *
     * @param cdcConfigId CDC 配置 ID
     */
    void enableCdcSync(String cdcConfigId);

    /**
     * 为指定 CDC 配置禁用 Flink 同步
     * <p>
     * 禁用该 CDC 配置。如果该数据源下没有其他启用的表，则停止 Flink 作业。
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
     * 检查所有运行中的 Flink 作业状态，更新数据库。
     */
    void refreshSyncStatus();

    /**
     * 根据数据源名称查询 Flink 作业
     *
     * @param dsName 数据源名称
     * @return Flink 作业，不存在时返回 null
     */
    com.cyan.dataman.domain.cdc.CdcFlinkJob findByDsName(String dsName);
}
