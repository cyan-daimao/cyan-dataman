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

//    @Bean
//    public SparkSession getSparkSession() {
//        return SparkSession.builder()
//                .appName(appName)
//                .master("local[*]")
//                // 启用Iceberg Spark扩展（支持MERGE INTO, UPDATE等）
//                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
//                // 定义名为 'local' 的 Iceberg Catalog，类型为 hive（关键！）
//                .config("spark.sql.catalog.iceberg", "org.apache.iceberg.spark.SparkCatalog")
//                .config("spark.sql.catalog.iceberg.type", "rest")  // 使用 Hive Metastore
//                .config("spark.sql.catalog.iceberg.uri", "http://iceberg-gravitino.cyan.com/iceberg")  // 改成你的 Hive Metastore 地址
////                .config("spark.sql.catalog.iceberg.warehouse", "s3://warehouse") // AI经常会生成这个配置但是这个配置不能添加
//                // ⭐ 关键：设为默认 catalog，这样就不需要写 local.xxx
//                .config("spark.sql.defaultCatalog", "iceberg")
//                // MinIO S3 配置（保持不变）
//                .config("spark.hadoop.fs.s3a.endpoint", "http://10.0.0.2:9000")
//                .config("spark.hadoop.fs.s3a.access.key", "rustfsadmin")
//                .config("spark.hadoop.fs.s3a.secret.key", "rustfsadmin")
//                .config("spark.hadoop.fs.s3a.path.style.access", "true")
//

    /// /                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
    /// /                .config("spark.hadoop.fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider")
//                .getOrCreate();
//    }
    @Bean
    public SparkSession getSparkSession() {
        return SparkSession.builder()
                .appName(appName)
                .master("local[*]")
                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .config("spark.hadoop.fs.s3a.path.style.access", "true")
                .config("spark.hadoop.fs.s3a.endpoint", "http://rustfs.cyan.com")
                .config("spark.hadoop.fs.s3a.endpoint.region", "eu-west-3")
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
                .config("spark.sql.catalog.rest", "org.apache.iceberg.spark.SparkCatalog")
                .config("spark.sql.catalog.rest.catalog-impl", "org.apache.iceberg.rest.RESTCatalog")
                .config("spark.sql.catalog.rest.uri", "http://iceberg-gravitino.cyan.com/iceberg")
                .config("spark.sql.catalog.rest.s3.endpoint", "http://rustfs.cyan.com")
                .config("spark.sql.catalog.rest.s3.path-style-access", "true")
                .config("spark.sql.catalog.rest.s3.endpoint.region", "eu-west-3")
//                .config("spark.sql.catalog.rest.default-namespace", "default")
//                .config("spark.sql.catalog.rest.warehouse", "s3://warehouse/")
                .config("spark.sql.defaultCatalog", "rest")
                .getOrCreate();
    }
}
