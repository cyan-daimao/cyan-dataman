package com.cyan.dataman.infra.config;

import org.apache.gravitino.client.GravitinoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GravitinoConfig {

    @Value("${gravitino.server.url}")
    private String gravitinoServerUrl;

    /**
     * 初始化 Gravitino 客户端
     */
    @Bean
    public GravitinoClient gravitinoClient() {
        return GravitinoClient.builder(gravitinoServerUrl)
                .withMetalake("cyan")
                .build();
    }
}