package com.cyan.dataman.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 异步请求配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Configuration
public class WebMvcAsyncConfig implements WebMvcConfigurer {

    /**
     * 异步请求超时时间，默认 10 分钟
     */
    @Value("${web.mvc.async.request-timeout:600000}")
    private Long requestTimeout;

    /**
     * 配置异步请求超时时间
     *
     * @param configurer 异步支持配置器
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(requestTimeout);
    }
}
