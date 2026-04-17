package com.cyan.dataman.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spark 执行环境配置
 * <p>
 * 作为配置持有者，提供工厂方法创建 SparkSession。
 * 支持 local 和 connect 模式：
 * - local: 嵌入式 SparkSession，用于 Iceberg 表维护等内部操作
 * - connect: 通过 Spark Connect 协议连接远端 Spark 集群，用于 CDC 同步
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
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

    @Value("${spark.connect.url:}")
    private String sparkConnectUrl;

    /**
     * 工厂方法：创建 Spark Connect 客户端 SparkSession
     * <p>
     * 通过 Spark Connect 协议连接远端 Spark 集群，驱动运行在远端，Spring Boot 重启不影响。
     */
    public SparkSession createSparkSession() {
        SparkSession.Builder builder = SparkSession.builder()
                .appName(appName)
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
                .config("spark.sql.catalog.rest", "org.apache.iceberg.spark.SparkCatalog")
                .config("spark.sql.catalog.rest.catalog-impl", "org.apache.iceberg.rest.RESTCatalog")
                .config("spark.sql.catalog.rest.uri", icebergRestUri)
                .config("spark.sql.catalog.rest.s3.endpoint", rustfsEndpoint)
                .config("spark.sql.catalog.rest.s3.path-style-access", "true")
                .config("spark.sql.catalog.rest.s3.endpoint.region", "cn-north-1")
                .config("spark.sql.catalog.rest.s3.access-key-id", rustfsAccessKey)
                .config("spark.sql.catalog.rest.s3.secret-access-key", rustfsSecretKey)
                .config("spark.sql.defaultCatalog", "rest");

        if (sparkConnectUrl != null && !sparkConnectUrl.isBlank()) {
            builder.remote(sparkConnectUrl);
            log.info("创建 Spark Connect 客户端, url: {}", sparkConnectUrl);
        } else {
            builder.master("local[*]");
            log.info("创建本地 SparkSession (未配置 spark.connect.url)");
        }

        return builder.getOrCreate();
    }

    /**
     * 创建本地模式 SparkSession（用于 Iceberg 表维护等内部操作）
     */
    public SparkSession createLocalSparkSession() {
        return SparkSession.builder()
                .appName(appName + "-maintenance")
                .master("local[*]")
                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .config("spark.hadoop.fs.s3a.path.style.access", "true")
                .config("spark.hadoop.fs.s3a.endpoint", rustfsEndpoint)
                .config("spark.hadoop.fs.s3a.endpoint.region", "cn-north-1")
                .config("spark.hadoop.fs.s3a.access.key", rustfsAccessKey)
                .config("spark.hadoop.fs.s3a.secret.key", rustfsSecretKey)
                .config("spark.hadoop.fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
                .config("spark.hadoop.fs.s3.path.style.access", "true")
                .config("spark.hadoop.fs.s3.endpoint", rustfsEndpoint)
                .config("spark.hadoop.fs.s3.endpoint.region", "cn-north-1")
                .config("spark.hadoop.fs.s3.access.key", rustfsAccessKey)
                .config("spark.hadoop.fs.s3.secret.key", rustfsSecretKey)
                .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
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

    public String getSparkConnectUrl() {
        return sparkConnectUrl;
    }

    /**
     * 本地模式 SparkSession Bean（用于 Iceberg 表维护等内部操作）
     */
    @org.springframework.context.annotation.Bean
    public SparkSession localSparkSession() {
        return createLocalSparkSession();
    }
}
