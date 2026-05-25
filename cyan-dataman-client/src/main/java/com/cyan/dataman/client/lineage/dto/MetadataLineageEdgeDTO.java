package com.cyan.dataman.client.lineage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据血缘边 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MetadataLineageEdgeDTO {

    /**
     * 源节点唯一键
     */
    private String sourceKey;

    /**
     * 目标节点唯一键
     */
    private String targetKey;

    /**
     * 边类型
     */
    private String edgeType;

    /**
     * 来源服务名
     */
    private String serviceName;

    /**
     * 来源业务ID
     */
    private String refId;

    /**
     * 扩展属性 JSON
     */
    private String propertiesJson;
}
