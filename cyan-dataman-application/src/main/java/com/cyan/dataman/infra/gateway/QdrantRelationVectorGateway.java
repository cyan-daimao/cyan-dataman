package com.cyan.dataman.infra.gateway;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.infra.gateway.model.RelationVectorDocument;
import com.cyan.dataman.infra.gateway.model.RelationVectorSearchResult;
import com.cyan.dataman.infra.rpc.qdrant.QdrantRelationRPC;
import com.cyan.dataman.infra.rpc.qdrant.request.QdrantCreateCollectionRequest;
import com.cyan.dataman.infra.rpc.qdrant.request.QdrantDeletePointsRequest;
import com.cyan.dataman.infra.rpc.qdrant.request.QdrantSearchRequest;
import com.cyan.dataman.infra.rpc.qdrant.request.QdrantUpsertPointsRequest;
import com.cyan.dataman.infra.rpc.qdrant.response.QdrantSearchResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Qdrant 关联推荐向量网关
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantRelationVectorGateway implements RelationVectorGateway {

    private final QdrantRelationRPC qdrantRelationRPC;

    /**
     * 是否启用 Qdrant 关联推荐
     */
    @Value("${qdrant.relation.enabled:false}")
    private Boolean enabled;

    /**
     * Qdrant 服务地址
     */
    @Value("${qdrant.relation.base-url:}")
    private String baseUrl;

    /**
     * Qdrant API Key
     */
    @Value("${qdrant.relation.api-key:}")
    private String apiKey;

    /**
     * Qdrant 集合名称
     */
    @Value("${qdrant.relation.collection:metadata_table_relation}")
    private String collection;

    /**
     * 召回数量
     */
    @Value("${qdrant.relation.top-k:30}")
    private Integer topK;

    /**
     * 相似度阈值
     */
    @Value("${qdrant.relation.score-threshold:0.3}")
    private Double scoreThreshold;

    /**
     * 是否可用
     */
    @Override
    public boolean available() {
        return Boolean.TRUE.equals(enabled)
                && baseUrl != null && !baseUrl.isBlank()
                && collection != null && !collection.isBlank();
    }

    /**
     * 默认召回数量
     */
    @Override
    public int topK() {
        return Optional.ofNullable(topK).orElse(30);
    }

    /**
     * 确保集合存在
     */
    @Override
    public void ensureCollection(int dimension) {
        if (!available() || dimension <= 0) {
            return;
        }
        try {
            QdrantCreateCollectionRequest request = new QdrantCreateCollectionRequest()
                    .setVectors(new QdrantCreateCollectionRequest.VectorConfig()
                            .setSize(dimension)
                            .setDistance("Cosine"));
            qdrantRelationRPC.createCollection(apiKey(), collection, request);
        } catch (FeignException.Conflict ignored) {
            // 集合已存在
        } catch (FeignException e) {
            log.warn("Qdrant 集合初始化失败, baseUrl: {}, collection: {}, status: {}, response: {}",
                    baseUrl, collection, e.status(), e.contentUTF8());
            throw new SilentException("Qdrant 集合初始化失败");
        } catch (Exception e) {
            log.warn("Qdrant 集合初始化异常, baseUrl: {}, collection: {}", baseUrl, collection, e);
            throw new SilentException("Qdrant 集合初始化异常");
        }
    }

    /**
     * 写入向量文档
     */
    @Override
    public void upsert(RelationVectorDocument document) {
        if (!available() || document == null || document.getVector() == null || document.getVector().isEmpty()) {
            return;
        }
        try {
            QdrantUpsertPointsRequest.Point point = new QdrantUpsertPointsRequest.Point()
                    .setId(document.getPointId())
                    .setVector(document.getVector())
                    .setPayload(document.getPayload());
            qdrantRelationRPC.upsertPoints(apiKey(), collection, true,
                    new QdrantUpsertPointsRequest().setPoints(List.of(point)));
        } catch (FeignException e) {
            log.warn("Qdrant 向量写入失败, baseUrl: {}, collection: {}, status: {}, response: {}",
                    baseUrl, collection, e.status(), e.contentUTF8());
            throw new SilentException("Qdrant 向量写入失败");
        } catch (Exception e) {
            log.warn("Qdrant 向量写入异常, baseUrl: {}, collection: {}", baseUrl, collection, e);
            throw new SilentException("Qdrant 向量写入异常");
        }
    }

    /**
     * 删除向量文档
     */
    @Override
    public void delete(String pointId) {
        if (!available() || pointId == null || pointId.isBlank()) {
            return;
        }
        try {
            qdrantRelationRPC.deletePoints(apiKey(), collection, true,
                    new QdrantDeletePointsRequest().setPoints(List.of(pointId)));
        } catch (FeignException e) {
            log.warn("Qdrant 向量删除失败, baseUrl: {}, collection: {}, pointId: {}, status: {}, response: {}",
                    baseUrl, collection, pointId, e.status(), e.contentUTF8());
            throw new SilentException("Qdrant 向量删除失败");
        } catch (Exception e) {
            log.warn("Qdrant 向量删除异常, baseUrl: {}, collection: {}, pointId: {}", baseUrl, collection, pointId, e);
            throw new SilentException("Qdrant 向量删除异常");
        }
    }

    /**
     * 搜索相似表
     */
    @Override
    public List<RelationVectorSearchResult> search(List<Double> vector, int limit) {
        if (!available() || vector == null || vector.isEmpty()) {
            return List.of();
        }
        try {
            QdrantSearchRequest request = new QdrantSearchRequest()
                    .setVector(vector)
                    .setLimit(limit)
                    .setScoreThreshold(scoreThreshold)
                    .setWithPayload(true);
            QdrantSearchResponse response = qdrantRelationRPC.search(apiKey(), collection, request);
            return Optional.ofNullable(response)
                    .map(QdrantSearchResponse::getResult)
                    .orElse(List.of())
                    .stream()
                    .map(point -> new RelationVectorSearchResult()
                            .setTableId(point.getPayload() == null ? null : point.getPayload().getString("tableId"))
                            .setCatalog(point.getPayload() == null ? null : point.getPayload().getString("catalog"))
                            .setSchema(point.getPayload() == null ? null : point.getPayload().getString("schema"))
                            .setTable(point.getPayload() == null ? null : point.getPayload().getString("table"))
                            .setScore(point.getScore()))
                    .toList();
        } catch (FeignException e) {
            log.warn("Qdrant 向量搜索失败, baseUrl: {}, collection: {}, status: {}, response: {}",
                    baseUrl, collection, e.status(), e.contentUTF8());
            throw new SilentException("Qdrant 向量搜索失败");
        } catch (Exception e) {
            log.warn("Qdrant 向量搜索异常, baseUrl: {}, collection: {}", baseUrl, collection, e);
            throw new SilentException("Qdrant 向量搜索异常");
        }
    }

    /**
     * 获取 API Key
     */
    private String apiKey() {
        return apiKey == null ? "" : apiKey;
    }
}
