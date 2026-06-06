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
            if (answerConsumer != null) {
                answerConsumer.accept("\n[dify_config] baseUrl=" + baseUrl + ", responseMode=streaming, user="
                        + Optional.ofNullable(user).filter(item -> !item.isBlank()).orElse("cyan-dataman-quality") + "\n");
            }
            Response response = difyQualityRPC.chat("Bearer " + apiKey, request);
            if (answerConsumer != null) {
                answerConsumer.accept("\n[dify_http_response] status=" + (response == null ? "null" : response.status())
                        + ", reason=" + (response == null ? "null" : response.reason())
                        + ", bodyPresent=" + (response != null && response.body() != null)
                        + "\n");
            }
            validateSuccessfulResponse(response);
            return readStreamingAnswer(response, answerConsumer);
        } catch (FeignException e) {
            String response = readableResponse(e.contentUTF8());
            log.warn("Dify 数据质量推荐调用失败, baseUrl: {}, status: {}, response: {}",
                    baseUrl, e.status(), response);
            throw new SilentException("Dify 数据质量推荐调用失败，status=" + e.status() + "，response=" + response);
        } catch (IOException e) {
            log.warn("Dify 数据质量推荐流式响应读取失败, baseUrl: {}", baseUrl, e);
            throw new SilentException("Dify 数据质量推荐响应读取失败");
        }
    }

    /**
     * 格式化可读响应
     */
    private String readableResponse(String response) {
        if (response == null || response.isBlank()) {
            return "empty";
        }
        String text = response.replaceAll("\\s+", " ").trim();
        if (text.length() > 500) {
            return text.substring(0, 500) + "...";
        }
        return text;
    }

    /**
     * 校验 Dify HTTP 响应状态
     */
    private void validateSuccessfulResponse(Response response) throws IOException {
        if (response == null) {
            throw new SilentException("Dify 数据质量推荐调用失败，response=null");
        }
        if (response.status() >= 200 && response.status() < 300) {
            return;
        }
        String body = response.body() == null ? "empty" : readableResponse(readResponseBody(response));
        throw new SilentException("Dify 数据质量推荐调用失败，status=" + response.status()
                + "，reason=" + Optional.ofNullable(response.reason()).orElse("")
                + "，response=" + body);
    }

    /**
     * 读取普通响应体
     */
    private String readResponseBody(Response response) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().asInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        }
    }

    /**
     * 读取 Dify streaming 响应中的 answer 片段
     */
    private String readStreamingAnswer(Response response, Consumer<String> answerConsumer) throws IOException {
        if (response == null || response.body() == null) {
            if (answerConsumer != null) {
                answerConsumer.accept("\n[dify_stream_summary] responseBodyEmpty=true\n");
            }
            return null;
        }
        StringBuilder answer = new StringBuilder();
        StringBuilder workflowOutput = new StringBuilder();
        int lineCount = 0;
        int dataLineCount = 0;
        StringBuilder rawSample = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().asInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                appendRawSample(rawSample, line);
                String trimmed = line.trim();
                if (!trimmed.startsWith("data:")) {
                    continue;
                }
                dataLineCount++;
                String json = trimmed.substring(5).trim();
                if (json.isBlank() || "[DONE]".equals(json)) {
                    continue;
                }
                JSONObject event = JSON.parseObject(json);
                String eventName = event.getString("event");
                String trace = extractEventTrace(event);
                if (hasText(trace) && answerConsumer != null) {
                    answerConsumer.accept(trace);
                }
                String analysis = extractAnalysisPart(event);
                if (hasText(analysis) && answerConsumer != null) {
                    answerConsumer.accept(analysis);
                }
                String part = extractAnswerPart(event);
                if (part != null) {
                    answer.append(part);
                    if (answerConsumer != null) {
                        answerConsumer.accept(part);
                    }
                }
                if ("workflow_finished".equals(eventName)) {
                    String output = extractWorkflowOutput(event);
                    if (hasText(output)) {
                        workflowOutput.setLength(0);
                        workflowOutput.append(output);
                    }
                }
                if ("error".equals(eventName)) {
                    throw new SilentException("Dify 数据质量推荐调用失败：" + event.getString("message"));
                }
            }
        }
        if (answerConsumer != null) {
            answerConsumer.accept("\n[dify_stream_summary] lineCount=" + lineCount
                    + ", dataLineCount=" + dataLineCount
                    + ", answerLength=" + answer.length()
                    + ", workflowOutputLength=" + workflowOutput.length()
                    + ", rawSample=" + trimForDisplay(rawSample.toString()) + "\n");
        }
        if (answer.isEmpty() && !workflowOutput.isEmpty()) {
            String output = workflowOutput.toString();
            if (answerConsumer != null) {
                answerConsumer.accept(output);
            }
            return output;
        }
        return answer.toString();
    }

    /**
     * 记录原始响应样例
     */
    private void appendRawSample(StringBuilder rawSample, String line) {
        if (rawSample.length() >= 2000) {
            return;
        }
        rawSample.append(line).append("\n");
    }

    /**
     * 提取 Dify 事件追踪信息
     */
    private String extractEventTrace(JSONObject event) {
        String eventName = event.getString("event");
        if (!hasText(eventName)) {
            return null;
        }
        StringBuilder builder = new StringBuilder("\n[dify_event] ").append(eventName);
        JSONObject data = event.getJSONObject("data");
        if (data != null && !data.isEmpty()) {
            builder.append(" dataKeys=").append(data.keySet());
            JSONObject outputs = data.getJSONObject("outputs");
            if (outputs != null && !outputs.isEmpty()) {
                builder.append(" outputKeys=").append(outputs.keySet());
            }
        }
        if (event.containsKey("answer")) {
            builder.append(" answerLength=").append(Optional.ofNullable(event.getString("answer")).orElse("").length());
        }
        return builder.append("\n").toString();
    }

    /**
     * 提取分析过程片段
     */
    private String extractAnalysisPart(JSONObject event) {
        String eventName = event.getString("event");
        if ("agent_thought".equals(eventName)) {
            return extractAgentThought(event);
        }
        if ("workflow_started".equals(eventName)) {
            return "\n[workflow] 工作流开始\n";
        }
        if ("node_started".equals(eventName)) {
            return extractNodeStatus(event, "开始");
        }
        if ("node_finished".equals(eventName)) {
            return extractNodeFinished(event);
        }
        return null;
    }

    /**
     * 提取 Agent 分析过程
     */
    private String extractAgentThought(JSONObject event) {
        StringBuilder builder = new StringBuilder();
        appendAnalysisLine(builder, "thought", event.getString("thought"));
        appendAnalysisLine(builder, "tool", event.getString("tool"));
        appendAnalysisLine(builder, "tool_input", event.getString("tool_input"));
        appendAnalysisLine(builder, "observation", event.getString("observation"));
        if (builder.isEmpty()) {
            return null;
        }
        return "\n[agent_thought]\n" + builder;
    }

    /**
     * 提取节点状态
     */
    private String extractNodeStatus(JSONObject event, String status) {
        JSONObject data = event.getJSONObject("data");
        if (data == null) {
            return null;
        }
        String title = Optional.ofNullable(data.getString("title")).orElse(data.getString("node_id"));
        if (!hasText(title)) {
            return null;
        }
        return "\n[node] " + title + " " + status + "\n";
    }

    /**
     * 提取节点完成信息
     */
    private String extractNodeFinished(JSONObject event) {
        JSONObject data = event.getJSONObject("data");
        if (data == null) {
            return null;
        }
        String title = Optional.ofNullable(data.getString("title")).orElse(data.getString("node_id"));
        String status = Optional.ofNullable(data.getString("status")).orElse("finished");
        StringBuilder builder = new StringBuilder();
        if (hasText(title)) {
            builder.append("\n[node] ").append(title).append(" ").append(status).append("\n");
        }
        JSONObject outputs = data.getJSONObject("outputs");
        if (outputs != null && !outputs.isEmpty()) {
            builder.append(trimForDisplay(JSON.toJSONString(outputs))).append("\n");
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    /**
     * 提取普通消息片段
     */
    private String extractAnswerPart(JSONObject event) {
        String eventName = event.getString("event");
        if ("message".equals(eventName) || "agent_message".equals(eventName) || "message_replace".equals(eventName)) {
            return event.getString("answer");
        }
        return null;
    }

    /**
     * 追加分析行
     */
    private void appendAnalysisLine(StringBuilder builder, String label, String value) {
        if (!hasText(value)) {
            return;
        }
        builder.append(label).append(": ").append(trimForDisplay(value)).append("\n");
    }

    /**
     * 截断展示文本
     */
    private String trimForDisplay(String text) {
        if (!hasText(text)) {
            return "";
        }
        String value = text.trim();
        if (value.length() > 2000) {
            return value.substring(0, 2000) + "...";
        }
        return value;
    }

    /**
     * 提取工作流最终输出
     */
    private String extractWorkflowOutput(JSONObject event) {
        JSONObject data = event.getJSONObject("data");
        if (data == null) {
            return null;
        }
        JSONObject outputs = data.getJSONObject("outputs");
        if (outputs == null || outputs.isEmpty()) {
            return null;
        }
        for (String key : List.of("answer", "text", "result", "output", "json", "suggestions")) {
            Object value = outputs.get(key);
            String text = stringifyOutput(value);
            if (hasText(text)) {
                return text;
            }
        }
        return JSON.toJSONString(outputs);
    }

    /**
     * 输出值转字符串
     */
    private String stringifyOutput(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        return JSON.toJSONString(value);
    }

    /**
     * 判断字符串是否有内容
     */
    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
