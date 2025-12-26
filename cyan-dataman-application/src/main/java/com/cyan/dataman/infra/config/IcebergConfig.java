package com.cyan.dataman.infra.config;

import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.hive.HiveCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class IcebergConfig {

    @Bean
    public Catalog icebergCatalog() {
        // 1. 创建 Hadoop Configuration 并设置 S3A 参数（关键！）
        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
        hadoopConf.set("fs.s3a.endpoint", "http://10.0.0.2:9000");
        hadoopConf.set("fs.s3a.access.key", "rustfsadmin");
        hadoopConf.set("fs.s3a.secret.key", "rustfsadmin");
        hadoopConf.set("fs.s3a.path.style.access", "true");
        hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        // 明确指定凭证提供方式，避免回退到环境变量或 IAM
        hadoopConf.set("fs.s3a.aws.credentials.provider",
                "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");

        // 2. Iceberg Catalog 配置（仅包含 Iceberg 自己的参数）
        Map<String, String> catalogProps = new HashMap<>();
        catalogProps.put("type", "hive");
        catalogProps.put("uri", "thrift://10.0.0.2:9083");
        catalogProps.put("warehouse", "s3a://bigdata/warehouse"); // 注意：用 s3a://

        // 3. 创建并初始化 Catalog，同时绑定 Hadoop 配置
        HiveCatalog catalog = new HiveCatalog();
        catalog.setConf(hadoopConf); // 👈 这一步至关重要！
        catalog.initialize("warehouse", catalogProps);

        return catalog;
    }

}