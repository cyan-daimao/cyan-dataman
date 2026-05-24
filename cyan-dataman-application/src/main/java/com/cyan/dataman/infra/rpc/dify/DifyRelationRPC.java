package com.cyan.dataman.infra.rpc.dify;

import com.cyan.dataman.infra.rpc.dify.request.DifyChatMessageRequest;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Dify 关联推荐 RPC
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "dify-relation-rpc", contextId = "difyRelationRPC", url = "${dify.relation.base-url:}")
public interface DifyRelationRPC {

    /**
     * 阻塞模式发送对话消息
     *
     * @param authorization 认证头
     * @param request       请求体
     * @return 对话响应
     */
    @PostMapping("/chat-messages")
    Response chat(@RequestHeader("Authorization") String authorization,
                  @RequestBody DifyChatMessageRequest request);
}
