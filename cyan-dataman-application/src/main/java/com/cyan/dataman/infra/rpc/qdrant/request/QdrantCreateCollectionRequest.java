package com.cyan.dataman.infra.rpc.qdrant.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Qdrant 创建集合请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class QdrantCreateCollectionRequest {

    /**
     * 向量配置
     */
    private VectorConfig vectors;

    /**
     * 向量配置
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Accessors(chain = true)
    public static class VectorConfig {

        /**
         * 向量维度
         */
        private Integer size;

        /**
         * 距离函数
         */
        private String distance;
    }
}
