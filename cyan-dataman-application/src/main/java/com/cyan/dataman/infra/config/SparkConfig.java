package com.cyan.dataman.infra.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author cy.Y
 */
@Component
public class SparkConfig {
    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public SparkSession getSparkSession() {
        return SparkSession.builder()
                .appName(appName)
                .master("local[*]")

                // 启用Iceberg Spark扩展（支持MERGE INTO, UPDATE等）
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")

                // 定义名为 'local' 的 Iceberg Catalog，类型为 hive（关键！）
                .config("spark.sql.catalog.local", "org.apache.iceberg.spark.SparkCatalog")
                .config("spark.sql.catalog.local.type", "hive")  // 使用 Hive Metastore
                .config("spark.sql.catalog.local.uri", "thrift://10.0.0.2:9083")  // 改成你的 Hive Metastore 地址
                .config("spark.sql.catalog.local.warehouse", "s3a://bigdata/warehouse") // OSS/MinIO 上的数据存储根路径

                // ⭐ 关键：设为默认 catalog，这样就不需要写 local.xxx
                .config("spark.sql.defaultCatalog", "local")
                // MinIO S3 配置（保持不变）
                .config("spark.hadoop.fs.s3a.endpoint", "http://10.0.0.2:9000")
                .config("spark.hadoop.fs.s3a.access.key", "rustfsadmin")
                .config("spark.hadoop.fs.s3a.secret.key", "rustfsadmin")
                .config("spark.hadoop.fs.s3a.path.style.access", "true")
                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")

                .getOrCreate();
    }
}
