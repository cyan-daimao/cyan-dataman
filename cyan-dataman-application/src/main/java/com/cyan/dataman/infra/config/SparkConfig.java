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

    @Value("${iceberg.uri}")
    private String icebergRestUri;

    @Value("${rustfs.endpoint}")
    private String rustfsEndpoint;

    @Value("${rustfs.accessKey}")
    private String rustfsAccessKey;

    @Value("${rustfs.secretKey}")
    private String rustfsSecretKey;

    @Bean
    public SparkSession getSparkSession() {
        return SparkSession.builder()
                .appName(appName)
                .master("local[*]")
                // s3a 协议配置
                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .config("spark.hadoop.fs.s3a.path.style.access", "true")
                .config("spark.hadoop.fs.s3a.endpoint", rustfsEndpoint)
                .config("spark.hadoop.fs.s3a.endpoint.region", "cn-north-1")
                .config("spark.hadoop.fs.s3a.access.key", rustfsAccessKey)
                .config("spark.hadoop.fs.s3a.secret.key", rustfsSecretKey)
                // s3 协议映射到 S3AFileSystem（Iceberg 表 location 可能使用 s3:// 协议）
                .config("spark.hadoop.fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .config("spark.hadoop.fs.s3.path.style.access", "true")
                .config("spark.hadoop.fs.s3.endpoint", rustfsEndpoint)
                .config("spark.hadoop.fs.s3.endpoint.region", "cn-north-1")
                .config("spark.hadoop.fs.s3.access.key", rustfsAccessKey)
                .config("spark.hadoop.fs.s3.secret.key", rustfsSecretKey)
                // Iceberg Spark 扩展
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
                // REST Catalog 配置
                .config("spark.sql.catalog.rest", "org.apache.iceberg.spark.SparkCatalog")
                .config("spark.sql.catalog.rest.catalog-impl", "org.apache.iceberg.rest.RESTCatalog")
                .config("spark.sql.catalog.rest.uri", icebergRestUri)
                .config("spark.sql.catalog.rest.s3.endpoint", rustfsEndpoint)
                .config("spark.sql.catalog.rest.s3.path-style-access", "true")
                .config("spark.sql.catalog.rest.s3.endpoint.region", "cn-north-1")
                .config("spark.sql.catalog.rest.s3.access-key-id", rustfsAccessKey)
                .config("spark.sql.catalog.rest.s3.secret-access-key", rustfsSecretKey)
                .config("spark.sql.defaultCatalog", "rest")
                .getOrCreate();
    }
}
