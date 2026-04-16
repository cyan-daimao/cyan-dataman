package com.cyan.dataman.infra.config;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * flink session
 * @author cy.Y
 * @since v1.0.0
 */
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

    @Bean
    public StreamExecutionEnvironment streamExecutionEnvironment() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

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
        checkpointConfig.setCheckpointingMode(org.apache.flink.streaming.api.CheckpointingMode.EXACTLY_ONCE);

        return env;
    }
}
