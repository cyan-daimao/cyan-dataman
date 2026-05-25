package com.cyan.dataman.infra.rpc.embedding.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Embedding 请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddingRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 输入文本
     */
    private String input;

    /**
     * 自定义向量维度
     */
    private Integer dimensions;
}
