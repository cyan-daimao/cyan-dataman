package com.cyan.dataman.application.metadata.lineage.cmd;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 元数据血缘同步命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class MetadataLineageSyncCmd {

    /**
     * 来源服务名
     */
    private String serviceName;

    /**
     * 来源业务ID
     */
    private String refId;

    /**
     * 节点列表
     */
    private List<LineageNodeCmd> nodes;

    /**
     * 边列表
     */
    private List<LineageEdgeCmd> edges;

    /**
     * 血缘节点命令
     */
    @Data
    @Accessors(chain = true)
    public static class LineageNodeCmd {

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
     * 血缘边命令
     */
    @Data
    @Accessors(chain = true)
    public static class LineageEdgeCmd {

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
