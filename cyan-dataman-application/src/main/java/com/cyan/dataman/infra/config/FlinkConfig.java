package com.cyan.dataman.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * flink session
 * @author cy.Y
 * @since v1.0.0
 */
@Slf4j
@Configuration
public class FlinkConfig {
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

    /**
     * Checkpoint 间隔（毫秒）
     */
    @Value("${flink.checkpoint.interval:60000}")
    private long checkpointInterval;

    /**
     * Checkpoint 超时时间（毫秒）
     */
    @Value("${flink.checkpoint.timeout:600000}")
    private long checkpointTimeout;

    /**
     * Checkpoint 存储目录
     */
    @Value("${flink.checkpoint.dir:}")
    private String checkpointDir;

    @Bean
    public StreamExecutionEnvironment streamExecutionEnvironment() {
        // 配置 Flink 参数
        org.apache.flink.configuration.Configuration flinkConfig = new org.apache.flink.configuration.Configuration();

        // 注册 S3 文件系统（基于 Hadoop S3A），用于 checkpoint 存储
        flinkConfig.setString("s3.endpoint", rustfsEndpoint);
        flinkConfig.setString("s3.access-key", rustfsAccessKey);
        flinkConfig.setString("s3.secret-key", rustfsSecretKey);
        flinkConfig.setString("s3.path.style.access", "true");

        // 配置 Checkpoint 存储目录（统一使用 S3/RustFS）
        flinkConfig.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, checkpointDir);
        log.info("Flink Checkpoint 存储目录: {}", checkpointDir);

        // 作业取消后保留 checkpoint，以便恢复
        flinkConfig.set(CheckpointingOptions.EXTERNALIZED_CHECKPOINT_RETENTION,
                ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION);

        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(flinkConfig);

        // 配置 Checkpoint（精准一次语义）
        env.enableCheckpointing(checkpointInterval);

        // 获取 Checkpoint 配置
        var checkpointConfig = env.getCheckpointConfig();

        // 设置超时时间
        checkpointConfig.setCheckpointTimeout(checkpointTimeout);

        // 设置最小暂停间隔
        checkpointConfig.setMinPauseBetweenCheckpoints(500);

        // 设置并发 Checkpoint 数量
        checkpointConfig.setMaxConcurrentCheckpoints(1);

        // 精准一次语义
        checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);

        // 启用 unaligned checkpoints（提高性能）
        checkpointConfig.enableUnalignedCheckpoints();

        return env;
    }
}
