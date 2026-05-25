package com.cyan.dataman.infra.rpc.qdrant.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Qdrant 搜索请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class QdrantSearchRequest {

    /**
     * 查询向量
     */
    private List<Double> vector;

    /**
     * 返回数量
     */
    private Integer limit;

    /**
     * 分数阈值
     */
    @JsonProperty("score_threshold")
    private Double scoreThreshold;

    /**
     * 是否返回载荷
     */
    @JsonProperty("with_payload")
    private Boolean withPayload;
}
