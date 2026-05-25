package com.cyan.dataman.client.lineage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据血缘节点 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MetadataLineageNodeDTO {

    /**
     * 节点唯一键
     */
    private String nodeKey;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 来源服务名
     */
    private String serviceName;

    /**
     * 来源业务ID
     */
    private String refId;

    /**
     * 表唯一引用
     */
    private String tableRef;

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 扩展属性 JSON
     */
    private String propertiesJson;
}
