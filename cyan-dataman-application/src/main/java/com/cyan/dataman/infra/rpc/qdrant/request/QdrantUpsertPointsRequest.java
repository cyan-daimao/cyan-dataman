package com.cyan.dataman.infra.rpc.qdrant.request;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Qdrant 写入点请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class QdrantUpsertPointsRequest {

    /**
     * 点列表
     */
    private List<Point> points;

    /**
     * 点数据
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Accessors(chain = true)
    public static class Point {

        /**
         * 点ID
         */
        private String id;

        /**
         * 向量
         */
        private List<Double> vector;

        /**
         * 业务载荷
         */
        private JSONObject payload;
    }
}
