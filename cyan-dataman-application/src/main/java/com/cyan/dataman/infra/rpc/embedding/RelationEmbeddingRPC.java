package com.cyan.dataman.infra.rpc.embedding;

import com.cyan.dataman.infra.rpc.embedding.request.EmbeddingRequest;
import com.cyan.dataman.infra.rpc.embedding.response.EmbeddingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;

/**
 * 关联推荐 Embedding RPC
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "relation-embedding-rpc", contextId = "relationEmbeddingRPC", url = "${embedding.relation.base-url:}")
public interface RelationEmbeddingRPC {

    /**
     * 生成文本向量
     *
     * @param authorization 认证头
     * @param request       请求体
     * @return 向量响应
     */
    @PostMapping
    EmbeddingResponse embeddings(URI uri,
                                 @RequestHeader("Authorization") String authorization,
                                 @RequestBody EmbeddingRequest request);
}
