package com.cyan.dataman.infra.gateway;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.infra.rpc.embedding.RelationEmbeddingRPC;
import com.cyan.dataman.infra.rpc.embedding.request.EmbeddingRequest;
import com.cyan.dataman.infra.rpc.embedding.response.EmbeddingResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * 关联推荐 Embedding 网关
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RelationEmbeddingGateway {

    private final RelationEmbeddingRPC relationEmbeddingRPC;

    /**
     * 是否启用关联推荐 Embedding
     */
    @Value("${embedding.relation.enabled:false}")
    private Boolean enabled;

    /**
     * Embedding API Key
     */
    @Value("${embedding.relation.api-key:}")
    private String apiKey;

    /**
     * Embedding 服务地址
     */
    @Value("${embedding.relation.base-url:}")
    private String baseUrl;

    /**
     * Embedding 模型
     */
    @Value("${embedding.relation.model:}")
    private String model;

    /**
     * Embedding 向量维度
     */
    @Value("${embedding.relation.dimension:0}")
    private Integer dimension;

    /**
     * 是否可用
     */
    public boolean available() {
        return Boolean.TRUE.equals(enabled)
                && baseUrl != null && !baseUrl.isBlank()
                && model != null && !model.isBlank();
    }

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    public int dimension() {
        return Optional.ofNullable(dimension).orElse(0);
    }

    /**
     * 生成文本向量
     *
     * @param text 文本
     * @return 向量
     */
    public List<Double> embed(String text) {
        if (!available()) {
            return List.of();
        }
        try {
            EmbeddingResponse response = relationEmbeddingRPC.embeddings(
                    URI.create(embeddingEndpoint()),
                    apiKey == null || apiKey.isBlank() ? "" : "Bearer " + apiKey,
                    new EmbeddingRequest()
                            .setModel(model)
                            .setInput(text)
                            .setDimensions(dimension != null && dimension > 0 ? dimension : null)
            );
            return Optional.ofNullable(response)
                    .map(EmbeddingResponse::getData)
                    .orElse(List.of())
                    .stream()
                    .findFirst()
                    .map(EmbeddingResponse.EmbeddingData::getEmbedding)
                    .orElse(List.of());
        } catch (FeignException e) {
            log.warn("关联推荐 Embedding 调用失败, baseUrl: {}, status: {}, response: {}",
                    baseUrl, e.status(), e.contentUTF8());
            throw new SilentException("关联推荐 Embedding 调用失败");
        } catch (Exception e) {
            log.warn("关联推荐 Embedding 调用异常, baseUrl: {}", baseUrl, e);
            throw new SilentException("关联推荐 Embedding 调用异常");
        }
    }

    /**
     * 获取 Embedding 请求地址
     *
     * @return Embedding 请求地址
     */
    private String embeddingEndpoint() {
        String url = baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith("/embeddings")) {
            return url;
        }
        return url + "/embeddings";
    }
}
