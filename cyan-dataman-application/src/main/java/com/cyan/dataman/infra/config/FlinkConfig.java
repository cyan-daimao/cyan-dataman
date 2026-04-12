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

    @Bean
    public StreamExecutionEnvironment streamExecutionEnvironment() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 2. 关键：指向远程 Flink 集群地址
//        env.setRuntimeMode(RuntimeExecutionMode.STREAMING);
        org.apache.flink.configuration.Configuration config = new org.apache.flink.configuration.Configuration();
//        config.setString("execution.target", "remote");
//        config.set(JobManagerOptions.ADDRESS,"10.0.0.2");
//        config.set(JobManagerOptions.PORT,20031);
//        config.set(DeploymentOptions.ATTACHED, false);

        // 3. 生产必须开：Checkpoint（精准一次）
        env.enableCheckpointing(60000);

        return env;
    }
}
