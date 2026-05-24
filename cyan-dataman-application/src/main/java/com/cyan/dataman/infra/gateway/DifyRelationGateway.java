package com.cyan.dataman.infra.gateway;

import com.alibaba.fastjson2.JSONObject;
import com.cyan.dataman.infra.rpc.dify.DifyRelationRPC;
import com.cyan.dataman.infra.rpc.dify.request.DifyChatMessageRequest;
import com.cyan.dataman.infra.rpc.dify.response.DifyChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dify 关联推荐网关
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
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
        if (!available()) {
            return null;
        }
        DifyChatMessageRequest request = new DifyChatMessageRequest()
                .setInputs(new JSONObject())
                .setQuery(prompt)
                .setResponseMode("blocking")
                .setUser("cyan-dataman")
                .setFiles(List.of());
        DifyChatMessageResponse response = difyRelationRPC.chat("Bearer " + apiKey, request);
        return response == null ? null : response.getAnswer();
    }
}
