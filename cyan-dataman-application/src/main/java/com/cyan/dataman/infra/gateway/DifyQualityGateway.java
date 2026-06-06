package com.cyan.dataman.infra.gateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.infra.rpc.dify.DifyQualityRPC;
import com.cyan.dataman.infra.rpc.dify.request.DifyChatMessageRequest;
import feign.FeignException;
import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Dify 数据质量推荐网关
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DifyQualityGateway {

    private final DifyQualityRPC difyQualityRPC;

    /**
     * 是否启用 Dify 数据质量推荐
     */
    @Value("${dify.quality.enabled:false}")
    private Boolean enabled;

    /**
     * Dify API Key
     */
    @Value("${dify.quality.api-key:}")
    private String apiKey;

    /**
     * Dify 服务地址
     */
    @Value("${dify.quality.base-url:}")
    private String baseUrl;

    /**
     * Dify 用户标识
     */
    @Value("${dify.quality.user:cyan-dataman-quality}")
    private String user;

    /**
     * 判断是否可调用
     */
    public boolean available() {
        return Boolean.TRUE.equals(enabled)
                && apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    /**
     * 流式请求 Dify 生成数据质量规则推荐
     *
     * @param prompt         提示词
     * @param answerConsumer 回复片段消费者
     * @return 完整 Dify answer
     */
    public String streamSuggestRules(String prompt, Consumer<String> answerConsumer) {
        if (!available()) {
            return null;
        }
        DifyChatMessageRequest request = new DifyChatMessageRequest()
                .setInputs(new JSONObject())
                .setQuery(prompt)
                .setResponseMode("streaming")
                .setUser(Optional.ofNullable(user).filter(item -> !item.isBlank()).orElse("cyan-dataman-quality"))
                .setFiles(List.of());
        try {
            Response response = difyQualityRPC.chat("Bearer " + apiKey, request);
            return readStreamingAnswer(response, answerConsumer);
        } catch (FeignException e) {
            log.warn("Dify 数据质量推荐调用失败, baseUrl: {}, status: {}, response: {}",
                    baseUrl, e.status(), e.contentUTF8());
            throw new SilentException("Dify 数据质量推荐调用失败，请检查 dify.quality.base-url 是否指向 Dify API 地址");
        } catch (IOException e) {
            log.warn("Dify 数据质量推荐流式响应读取失败, baseUrl: {}", baseUrl, e);
            throw new SilentException("Dify 数据质量推荐响应读取失败");
        }
    }

    /**
     * 读取 Dify streaming 响应中的 answer 片段
     */
    private String readStreamingAnswer(Response response, Consumer<String> answerConsumer) throws IOException {
        if (response == null || response.body() == null) {
            return null;
        }
        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().asInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                String json = trimmed.substring(5).trim();
                if (json.isBlank() || "[DONE]".equals(json)) {
                    continue;
                }
                JSONObject event = JSON.parseObject(json);
                String eventName = event.getString("event");
                if ("message".equals(eventName) || "agent_message".equals(eventName)) {
                    String part = event.getString("answer");
                    if (part != null) {
                        answer.append(part);
                        if (answerConsumer != null) {
                            answerConsumer.accept(part);
                        }
                    }
                }
                if ("error".equals(eventName)) {
                    throw new SilentException("Dify 数据质量推荐调用失败：" + event.getString("message"));
                }
            }
        }
        return answer.toString();
    }
}
