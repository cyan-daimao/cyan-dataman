package com.cyan.dataman.application.metadata.lineage.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段血缘 BO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataFieldLineageBO {

    /**
     * 字段节点唯一键
     */
    private String fieldKey;

    /**
     * 产出或改写字段的上游任务
     */
    private List<LineageNodeBO> upstreamJobs = new ArrayList<>();

    /**
     * 读取字段的下游任务
     */
    private List<LineageNodeBO> downstreamJobs = new ArrayList<>();

    /**
     * 受字段影响的指标
     */
    private List<LineageNodeBO> metrics = new ArrayList<>();

    /**
     * 可视化边列表
     */
    private List<LineageEdgeBO> edges = new ArrayList<>();

    /**
     * 血缘节点 BO
     */
    @Data
    @Accessors(chain = true)
    public static class LineageNodeBO {

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

    /**
     * 血缘边 BO
     */
    @Data
    @Accessors(chain = true)
    public static class LineageEdgeBO {

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
}
