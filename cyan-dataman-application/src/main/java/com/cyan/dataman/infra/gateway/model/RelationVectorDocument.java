package com.cyan.dataman.infra.gateway.model;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 关联推荐向量文档
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class RelationVectorDocument {

    /**
     * 点ID
     */
    private String pointId;

    /**
     * 向量
     */
    private List<Double> vector;

    /**
     * 载荷
     */
    private JSONObject payload;
}
