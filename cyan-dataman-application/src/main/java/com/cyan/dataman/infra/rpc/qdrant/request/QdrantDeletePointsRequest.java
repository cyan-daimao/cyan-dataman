package com.cyan.dataman.infra.rpc.qdrant.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Qdrant 删除点请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class QdrantDeletePointsRequest {

    /**
     * 点ID列表
     */
    private List<String> points;
}
