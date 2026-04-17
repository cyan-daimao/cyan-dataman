package com.cyan.dataman.application.cdc.job;

import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.domain.cdc.CdcSparkTask;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.cdc.repository.CdcSparkTaskRepository;
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.repository.DsConfigRepository;
import com.cyan.dataman.enums.JobStatus;
import com.cyan.dataman.enums.SyncMode;
import com.cyan.dataman.infra.config.SparkConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spark 任务执行器（基于 Spark Connect）
 * <p>
 * 通过 Spark Connect 协议连接远端 Spark 集群执行 SQL：
 * 1. 动态注册 MySQL JDBC Catalog（如不存在）
 * 2. 根据 syncMode 自动生成 INSERT OVERWRITE / INSERT INTO SQL
 * 3. 执行 SQL 并更新任务状态
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class SparkJobExecutor {

    private final SparkConfig sparkConfig;
    private final CdcSparkTaskRepository cdcSparkTaskRepository;
    private final DsConfigRepository dsConfigRepository;

    public SparkJobExecutor(SparkConfig sparkConfig,
                            CdcSparkTaskRepository cdcSparkTaskRepository,
                            DsConfigRepository dsConfigRepository) {
        this.sparkConfig = sparkConfig;
        this.cdcSparkTaskRepository = cdcSparkTaskRepository;
        this.dsConfigRepository = dsConfigRepository;
    }

    /**
     * 执行 Spark 同步任务
     *
     * @param sparkJob Spark 作业配置
     * @param cdcConfig CDC 配置
     */
    public void executeSparkJob(CdcSparkJob sparkJob, CdcConfig cdcConfig) {
        // 创建任务实例
        CdcSparkTask task = new CdcSparkTask();
        task.setId(UUID.randomUUID().toString());
        task.setSparkJobId(sparkJob.getId());
        task.setCdcConfigId(cdcConfig.getId());
        task.setStatus(JobStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task = cdcSparkTaskRepository.save(task);

        DsConfig dsConfig = dsConfigRepository.findByName(cdcConfig.getDsName());
        if (dsConfig == null) {
            task.fail(cdcSparkTaskRepository, "数据源配置不存在: " + cdcConfig.getDsName());
            return;
        }

        SparkSession spark = null;
        try {
            spark = sparkConfig.createSparkSession();
            LocalDateTime startTime = LocalDateTime.now();
            task.start(cdcSparkTaskRepository, "spark-connect-" + task.getId());

            // 动态注册 MySQL Catalog
            String mysqlCatalogName = ensureMysqlCatalog(spark, dsConfig);

            // 解析 Iceberg 目标表信息
            String icebergTableName = cdcConfig.getIcebergTableName();
            String icebergSchema;
            String icebergTable;
            if (icebergTableName.contains(".")) {
                String[] parts = icebergTableName.split("\\.");
                icebergSchema = parts[0];
                icebergTable = parts[1];
            } else {
                icebergSchema = "ods";
                icebergTable = icebergTableName;
            }

            // 生成并执行 SQL
            String sql = buildSparkSql(sparkJob.getSyncMode(), mysqlCatalogName,
                    cdcConfig.getDbName(), cdcConfig.getTableName(),
                    icebergSchema, icebergTable);

            log.info("执行 Spark SQL: {}", sql);
            spark.sql(sql);

            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = Duration.between(startTime, endTime).getSeconds();
            task.success(cdcSparkTaskRepository, 0L);
            log.info("Spark 任务执行成功: sparkJobId={}, cdcConfigId={}, 耗时={}s",
                    sparkJob.getId(), cdcConfig.getId(), durationSeconds);
        } catch (Exception e) {
            log.error("Spark 任务执行失败: sparkJobId={}, cdcConfigId={}, error={}",
                    sparkJob.getId(), cdcConfig.getId(), e.getMessage(), e);
            task.fail(cdcSparkTaskRepository, e.getMessage());
        } finally {
            if (spark != null) {
                try {
                    spark.close();
                } catch (Exception e) {
                    log.debug("关闭 SparkSession 失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 动态注册 MySQL JDBC Catalog（如不存在）
     *
     * @return catalog 名称
     */
    private String ensureMysqlCatalog(SparkSession spark, DsConfig dsConfig) {
        // catalog 名称使用 dsName（替换特殊字符为下划线）
        String catalogName = "mysql_" + dsConfig.getName().replaceAll("[^a-zA-Z0-9]", "_");

        // CREATE OR REPLACE CATALOG 动态注册
        String createCatalogSql = String.format(
                "CREATE OR REPLACE CATALOG %s USING jdbc OPTIONS (" +
                        "url '%s', " +
                        "user '%s', " +
                        "password '%s', " +
                        "driver 'com.mysql.cj.jdbc.Driver')",
                catalogName, dsConfig.getUrl(), dsConfig.getUsername(), dsConfig.getPassword());

        spark.sql(createCatalogSql);
        log.info("动态注册 MySQL Catalog: {}", catalogName);
        return catalogName;
    }

    /**
     * 根据 syncMode 自动生成 Spark SQL
     */
    private String buildSparkSql(SyncMode syncMode, String mysqlCatalog,
                                 String mysqlDb, String mysqlTable,
                                 String icebergSchema, String icebergTable) {
        String target = String.format("rest.%s.%s", icebergSchema, icebergTable);
        String source = String.format("%s.%s.%s", mysqlCatalog, mysqlDb, mysqlTable);

        if (syncMode == SyncMode.OVERWRITE) {
            return String.format("INSERT OVERWRITE TABLE %s SELECT * FROM %s", target, source);
        } else {
            return String.format("INSERT INTO TABLE %s SELECT * FROM %s", target, source);
        }
    }
}
