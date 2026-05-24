package com.cyan.dataman.infra.gateway;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.infra.rpc.dify.DifyRelationRPC;
import com.cyan.dataman.infra.rpc.dify.request.DifyChatMessageRequest;
import feign.Response;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dify 关联推荐网关
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DifyRelationGateway {

    private final DifyRelationRPC difyRelationRPC;

    /**
     * 是否启用 Dify 关联推荐
     */
    @Value("${dify.relation.enabled:false}")
    private Boolean enabled;

    /**
     * Dify API Key
     */
    @Value("${dify.relation.api-key:}")
    private String apiKey;

    /**
     * Dify 服务地址
     */
    @Value("${dify.relation.base-url:}")
    private String baseUrl;

    /**
     * 判断是否可调用
     */
    public boolean available() {
        return Boolean.TRUE.equals(enabled)
                && apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank();
    }

    /**
     * 请求 Dify 生成关联推荐
     *
     * @param prompt 提示词
     * @return Dify answer
     */
    public String suggestRelations(String prompt) {
        return streamSuggestRelations(prompt, null);
    }

    /**
     * 流式请求 Dify 生成关联推荐
     *
     * @param prompt         提示词
     * @param answerConsumer 回复片段消费者
     * @return 完整 Dify answer
     */
    public String streamSuggestRelations(String prompt, Consumer<String> answerConsumer) {
        if (!available()) {
            return null;
        }
        DifyChatMessageRequest request = new DifyChatMessageRequest()
                .setInputs(new JSONObject())
                .setQuery(prompt)
                .setResponseMode("streaming")
                .setUser("cyan-dataman")
                .setFiles(List.of());
        try {
            Response response = difyRelationRPC.chat("Bearer " + apiKey, request);
            return readStreamingAnswer(response, answerConsumer);
        } catch (FeignException e) {
            log.warn("Dify 关联推荐调用失败, baseUrl: {}, status: {}, response: {}",
                    baseUrl, e.status(), e.contentUTF8());
            throw new SilentException("Dify 关联推荐调用失败，请检查 dify.relation.base-url 是否指向 Dify API 地址");
        } catch (IOException e) {
            log.warn("Dify 关联推荐流式响应读取失败, baseUrl: {}", baseUrl, e);
            throw new SilentException("Dify 关联推荐响应读取失败");
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
                    throw new SilentException("Dify 关联推荐调用失败：" + event.getString("message"));
                }
            }
        }
        return answer.toString();
    }
}
