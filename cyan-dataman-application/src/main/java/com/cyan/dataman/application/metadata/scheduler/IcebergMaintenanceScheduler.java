package com.cyan.dataman.application.metadata.scheduler;

import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.Table;
import org.apache.iceberg.spark.Spark3Util;
import org.apache.iceberg.spark.actions.SparkActions;
import org.apache.spark.sql.SparkSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * iceberg维护调度
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
@Slf4j
public class IcebergMaintenanceScheduler {

    private final MetadataTableService metadataTableService;
    private final SparkSession sparkSession;

    public IcebergMaintenanceScheduler(MetadataTableService metadataTableService, SparkSession sparkSession) {
        this.metadataTableService = metadataTableService;
        this.sparkSession = sparkSession;
    }

    /**
     * 合并小文件
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void maintenance() {
        List<MetadataTableBO> tableBOS = metadataTableService.list(new MetadataTableListQuery());
        for (MetadataTableBO tableBO : tableBOS) {
            try {
                Table table = Spark3Util.loadIcebergTable(sparkSession, "%s.%s".formatted(tableBO.getTable().getSchema(), tableBO.getTable().getName()));
                SparkActions actions = SparkActions.get(sparkSession);

                // 保留最近1秒的快照
                long olderThan = System.currentTimeMillis() - 1000;

                // 1. 过期快照（先删快照，再删文件，顺序不能乱）
                actions.expireSnapshots(table).expireOlderThan(olderThan).retainLast(10).execute();

                // 2. 合并小文件
                actions.rewriteDataFiles(table).option("target-file-size-bytes", Long.toString(128 * 1024 * 1024)).execute();

                // 3. 删除孤儿文件
                actions.deleteOrphanFiles(table).olderThan(olderThan).execute();

            } catch (Exception e) {
                log.error("合并小文件报错:", e);
                throw new RuntimeException(e);
            }
        }
    }
}
