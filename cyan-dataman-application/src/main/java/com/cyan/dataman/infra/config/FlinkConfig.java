package com.cyan.dataman.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.ExternalizedCheckpointRetention;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Flink 执行环境配置
 * <p>
 * 作为配置持有者，提供工厂方法创建 StreamExecutionEnvironment。
 * 每次提交作业时调用 {@link #createStreamExecutionEnvironment()} 创建新实例。
 * <p>
 * 支持 local 和 remote 模式：
 * - local: 在 Spring Boot 进程内本地运行 Flink 作业
 * - remote: 通过 RemoteStreamEnvironment 提交到远端 Flink 集群
 * <p>
 * S3 文件系统通过 hadoop-aws 的 S3AFileSystem 实现（由 core-site.xml 提供 duration 格式修复），
 * Flink 通过 HadoopFsFactory 回退机制使用 Hadoop 的 S3A。
 *
 * @author cy.Y
 * @since v1.0.0
 */
@Slf4j
@Configuration
public class FlinkConfig {

    @Value("${rustfs.endpoint}")
    private String rustfsEndpoint;

    @Value("${rustfs.accessKey}")
    private String rustfsAccessKey;

    @Value("${rustfs.secretKey}")
    private String rustfsSecretKey;

    @Value("${flink.mode:local}")
    private String flinkMode;

    @Value("${flink.rest.url:localhost:6123}")
    private String flinkRestUrl;

    @Value("${flink.checkpoint.interval:60000}")
    private long checkpointInterval;

    @Value("${flink.checkpoint.timeout:600000}")
    private long checkpointTimeout;

    @Value("${flink.checkpoint.dir:}")
    private String checkpointDir;

    /**
     * 工厂方法：创建新的 StreamExecutionEnvironment
     * <p>
     * 每次提交作业时调用，不复用。内部包含 S3、Checkpoint 等通用配置。
     */
    public StreamExecutionEnvironment createStreamExecutionEnvironment() {
        var flinkConfig = new org.apache.flink.configuration.Configuration();

        // S3 文件系统配置（用于 checkpoint 存储）
        // 通过 flink.hadoop. 前缀传递给 Hadoop Configuration，由 HadoopFsFactory 读取
        flinkConfig.setString("flink.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        flinkConfig.setString("flink.hadoop.fs.s3a.endpoint", rustfsEndpoint);
        flinkConfig.setString("flink.hadoop.fs.s3a.access.key", rustfsAccessKey);
        flinkConfig.setString("flink.hadoop.fs.s3a.secret.key", rustfsSecretKey);
        flinkConfig.setString("flink.hadoop.fs.s3a.path.style.access", "true");
        // s3:// scheme 映射到 S3AFileSystem
        flinkConfig.setString("flink.hadoop.fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        flinkConfig.setString("flink.hadoop.fs.s3.endpoint", rustfsEndpoint);
        flinkConfig.setString("flink.hadoop.fs.s3.access.key", rustfsAccessKey);
        flinkConfig.setString("flink.hadoop.fs.s3.secret.key", rustfsSecretKey);
        flinkConfig.setString("flink.hadoop.fs.s3.path.style.access", "true");

        // Checkpoint 存储目录
        flinkConfig.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, checkpointDir);

        // 作业取消后保留 checkpoint
        flinkConfig.set(CheckpointingOptions.EXTERNALIZED_CHECKPOINT_RETENTION,
                ExternalizedCheckpointRetention.RETAIN_ON_CANCELLATION);

        StreamExecutionEnvironment env;
        if ("remote".equalsIgnoreCase(flinkMode)) {
            String[] parts = flinkRestUrl.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6123;
            env = StreamExecutionEnvironment.createRemoteEnvironment(host, port, flinkConfig);
            log.info("创建远程 Flink 执行环境, JobManager: {}:{}", host, port);
        } else {
            env = StreamExecutionEnvironment.getExecutionEnvironment(flinkConfig);
            log.info("创建本地 Flink 执行环境");
        }

        // Checkpoint 配置
        env.enableCheckpointing(checkpointInterval);
        var checkpointConfig = env.getCheckpointConfig();
        checkpointConfig.setCheckpointTimeout(checkpointTimeout);
        checkpointConfig.setMinPauseBetweenCheckpoints(500);
        checkpointConfig.setMaxConcurrentCheckpoints(1);
        checkpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        checkpointConfig.enableUnalignedCheckpoints();

        return env;
    }

    public String getFlinkMode() {
        return flinkMode;
    }

    public String getFlinkRestUrl() {
        return flinkRestUrl;
    }
}
