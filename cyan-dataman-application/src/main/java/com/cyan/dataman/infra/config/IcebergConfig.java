//package com.cyan.dataman.infra.config;
//
//import org.apache.iceberg.catalog.Catalog;
//import org.apache.iceberg.hive.HiveCatalog;
//import org.apache.hadoop.hive.conf.HiveConf;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class IcebergConfig {
//
//    @Bean
//    public Catalog icebergCatalog() {
//        // 1. 初始化 Hadoop 配置（S3A 相关）
//        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
//        hadoopConf.set("fs.s3a.endpoint", "http://10.0.0.2:9000");
//        hadoopConf.set("fs.s3a.access.key", "rustfsadmin");
//        hadoopConf.set("fs.s3a.secret.key", "rustfsadmin");
//        hadoopConf.set("fs.s3a.path.style.access", "true");
//        hadoopConf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
//        hadoopConf.set("fs.s3a.aws.credentials.provider",
//                "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
//
//        // 2. 关键修复：初始化 HiveConf 并禁用配置文件加载
//        HiveConf hiveConf = new HiveConf(hadoopConf, HiveConf.class);
//        // 禁用 HiveConf 自动加载 hive-site.xml/hive-default.xml
//        hiveConf.set("hive.conf.file", "/dev/null"); // 指向空文件，绕过文件加载
//        hiveConf.set("hive.metastore.uris", "thrift://10.0.0.2:9083"); // 硬编码 metastore 地址
//        // 其他必要的 Hive 配置（按需补充）
//        hiveConf.set("hive.metastore.warehouse.dir", "s3a://bigdata/warehouse");
//        hiveConf.set("hive.exec.scratchdir", "s3a://bigdata/tmp/hive");
//
//        // 3. Iceberg Catalog 配置
//        Map<String, String> catalogProps = new HashMap<>();
//        catalogProps.put("type", "hive");
//        catalogProps.put("uri", "thrift://10.0.0.2:9083");
//        catalogProps.put("warehouse", "s3a://bigdata/warehouse");
//
//        // 4. 创建 HiveCatalog 并绑定自定义 HiveConf
//        HiveCatalog catalog = new HiveCatalog();
//        catalog.setConf(hiveConf); // 绑定包含 HiveConf 的配置
//        catalog.initialize("warehouse", catalogProps);
//
//        return catalog;
//    }
//
//}