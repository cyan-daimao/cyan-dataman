package com.cyan.dataman.infra.rpc.embedding.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Embedding 响应
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class EmbeddingResponse {

    /**
     * 响应对象类型
     */
    private String object;

    /**
     * 向量数据
     */
    private List<EmbeddingData> data;

    /**
     * Embedding 数据
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Accessors(chain = true)
    public static class EmbeddingData {

        /**
         * 向量对象类型
         */
        private String object;

        /**
         * 向量下标
         */
        private Integer index;

        /**
         * 向量值
         */
        private List<Double> embedding;
    }
}
