package com.cyan.dataman.infra.rpc.qdrant;

import com.cyan.dataman.infra.rpc.qdrant.request.QdrantCreateCollectionRequest;
import com.cyan.dataman.infra.rpc.qdrant.request.QdrantDeletePointsRequest;
import com.cyan.dataman.infra.rpc.qdrant.request.QdrantSearchRequest;
import com.cyan.dataman.infra.rpc.qdrant.request.QdrantUpsertPointsRequest;
import com.cyan.dataman.infra.rpc.qdrant.response.QdrantSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Qdrant 关联推荐 RPC
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "qdrant-relation-rpc", contextId = "qdrantRelationRPC", url = "${qdrant.relation.base-url:}")
public interface QdrantRelationRPC {

    /**
     * 创建或更新集合
     *
     * @param apiKey     API Key
     * @param collection 集合名称
     * @param request    请求体
     */
    @PutMapping("/collections/{collection}")
    void createCollection(@RequestHeader("api-key") String apiKey,
                          @PathVariable("collection") String collection,
                          @RequestBody QdrantCreateCollectionRequest request);

    /**
     * 写入点
     *
     * @param apiKey     API Key
     * @param collection 集合名称
     * @param wait       是否等待写入完成
     * @param request    请求体
     */
    @PutMapping("/collections/{collection}/points")
    void upsertPoints(@RequestHeader("api-key") String apiKey,
                      @PathVariable("collection") String collection,
                      @RequestParam("wait") Boolean wait,
                      @RequestBody QdrantUpsertPointsRequest request);

    /**
     * 搜索点
     *
     * @param apiKey     API Key
     * @param collection 集合名称
     * @param request    请求体
     * @return 搜索响应
     */
    @PostMapping("/collections/{collection}/points/search")
    QdrantSearchResponse search(@RequestHeader("api-key") String apiKey,
                                @PathVariable("collection") String collection,
                                @RequestBody QdrantSearchRequest request);

    /**
     * 删除点
     *
     * @param apiKey     API Key
     * @param collection 集合名称
     * @param wait       是否等待删除完成
     * @param request    请求体
     */
    @PostMapping("/collections/{collection}/points/delete")
    void deletePoints(@RequestHeader("api-key") String apiKey,
                      @PathVariable("collection") String collection,
                      @RequestParam("wait") Boolean wait,
                      @RequestBody QdrantDeletePointsRequest request);
}
