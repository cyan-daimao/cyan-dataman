package com.cyan.dataman.infra.rpc.qdrant.response;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Qdrant 搜索响应
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class QdrantSearchResponse {

    /**
     * 搜索结果
     */
    private List<ScoredPoint> result;

    /**
     * 搜索点
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Accessors(chain = true)
    public static class ScoredPoint {

        /**
         * 点ID
         */
        private String id;

        /**
         * 相似度分数
         */
        private Double score;

        /**
         * 载荷
         */
        private JSONObject payload;
    }
}
