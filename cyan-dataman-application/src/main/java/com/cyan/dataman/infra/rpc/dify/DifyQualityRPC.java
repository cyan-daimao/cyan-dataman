package com.cyan.dataman.infra.rpc.dify;

import com.cyan.dataman.infra.rpc.dify.request.DifyChatMessageRequest;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Dify 数据质量推荐RPC
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "dify-quality-rpc", contextId = "difyQualityRPC", url = "${dify.quality.base-url:}")
public interface DifyQualityRPC {

    /**
     * 发送对话消息
     *
     * @param authorization 授权头
     * @param request       请求体
     * @return 流式响应
     */
    @PostMapping("/chat-messages")
    Response chat(@RequestHeader("Authorization") String authorization,
                  @RequestBody DifyChatMessageRequest request);
}
