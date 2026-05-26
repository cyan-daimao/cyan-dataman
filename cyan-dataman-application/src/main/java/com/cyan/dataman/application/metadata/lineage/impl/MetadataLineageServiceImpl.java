package com.cyan.dataman.application.metadata.lineage.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.metadata.lineage.MetadataLineageService;
import com.cyan.dataman.application.metadata.lineage.bo.MetadataFieldLineageBO;
import com.cyan.dataman.application.metadata.lineage.cmd.MetadataLineageSyncCmd;
import com.cyan.dataman.application.metadata.lineage.convert.MetadataLineageAppConvert;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageEdge;
import com.cyan.dataman.domain.metadata.lineage.MetadataLineageNode;
import com.cyan.dataman.domain.metadata.lineage.query.MetadataFieldLineageQuery;
import com.cyan.dataman.domain.metadata.lineage.repository.MetadataLineageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * 元数据血缘应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MetadataLineageServiceImpl implements MetadataLineageService {

    private static final String EDGE_WRITES_FIELD = "WRITES_FIELD";
    private static final String EDGE_READS_FIELD = "READS_FIELD";
    private static final String EDGE_USES_FIELD = "USES_FIELD";
    private static final String EDGE_DERIVES_METRIC = "DERIVES_METRIC";
    private static final String NODE_METRIC = "METRIC";

    private final MetadataLineageRepository metadataLineageRepository;


    /**
     * 同步血缘节点与边
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sync(MetadataLineageSyncCmd cmd) {
        Assert.notBlank(cmd.getServiceName(), new SilentException("血缘来源服务不能为空"));
        Assert.notBlank(cmd.getRefId(), new SilentException("血缘来源业务ID不能为空"));
        metadataLineageRepository.deleteEdgesByServiceAndRefId(cmd.getServiceName(), cmd.getRefId());

        List<MetadataLineageNode> nodes = Optional.ofNullable(MetadataLineageAppConvert.INSTANCE.toNodes(cmd.getNodes())).orElse(List.of());
        for (MetadataLineageNode node : nodes) {
            fillSource(node, cmd.getServiceName(), cmd.getRefId());
            node.saveOrUpdate(metadataLineageRepository);
        }

        List<MetadataLineageEdge> edges = Optional.ofNullable(MetadataLineageAppConvert.INSTANCE.toEdges(cmd.getEdges())).orElse(List.of());
        Set<String> uniqueEdges = new HashSet<>();
        for (MetadataLineageEdge edge : edges) {
            fillSource(edge, cmd.getServiceName(), cmd.getRefId());
            String uniqueKey = edge.getSourceKey() + "->" + edge.getTargetKey() + ":" + edge.getEdgeType();
            if (uniqueEdges.add(uniqueKey)) {
                edge.save(metadataLineageRepository);
            }
        }
    }

    /**
     * 查询字段血缘
     */
    @Override
    public MetadataFieldLineageBO queryFieldLineage(MetadataFieldLineageQuery query) {
        validateQuery(query);
        String fieldKey = query.fieldNodeKey();
        int maxDepth = query.getMaxDepth() == null || query.getMaxDepth() < 1 ? 3 : query.getMaxDepth();

        List<MetadataLineageEdge> resultEdges = new ArrayList<>();
        List<MetadataLineageEdge> upstreamJobEdges = metadataLineageRepository.findEdgesByTargetAndType(fieldKey, EDGE_WRITES_FIELD);
        List<MetadataLineageEdge> downstreamJobEdges = metadataLineageRepository.findEdgesBySourceAndType(fieldKey, EDGE_READS_FIELD);
        resultEdges.addAll(upstreamJobEdges);
        resultEdges.addAll(downstreamJobEdges);

        List<MetadataLineageNode> upstreamJobs = findNodes(upstreamJobEdges.stream().map(MetadataLineageEdge::getSourceKey).toList());
        List<MetadataLineageNode> downstreamJobs = findNodes(downstreamJobEdges.stream().map(MetadataLineageEdge::getTargetKey).toList());

        MetricTraversalResult metricResult = traverseMetrics(fieldKey, maxDepth);
        resultEdges.addAll(metricResult.edges());

        return new MetadataFieldLineageBO()
                .setFieldKey(fieldKey)
                .setUpstreamJobs(toNodeBOList(upstreamJobs))
                .setDownstreamJobs(toNodeBOList(downstreamJobs))
                .setMetrics(toNodeBOList(metricResult.metrics()))
                .setEdges(toEdgeBOList(deduplicateEdges(resultEdges)));
    }

    /**
     * 填充节点来源
     */
    private void fillSource(MetadataLineageNode node, String serviceName, String refId) {
        if (node.getServiceName() == null || node.getServiceName().isBlank()) {
            node.setServiceName(serviceName);
        }
        if (node.getRefId() == null || node.getRefId().isBlank()) {
            node.setRefId(refId);
        }
    }

    /**
     * 填充边来源
     */
    private void fillSource(MetadataLineageEdge edge, String serviceName, String refId) {
        if (edge.getServiceName() == null || edge.getServiceName().isBlank()) {
            edge.setServiceName(serviceName);
        }
        if (edge.getRefId() == null || edge.getRefId().isBlank()) {
            edge.setRefId(refId);
        }
    }

    /**
     * 校验查询参数
     */
    private void validateQuery(MetadataFieldLineageQuery query) {
        Assert.notNull(query, new SilentException("字段血缘查询参数不能为空"));
        Assert.notBlank(query.getCatalog(), new SilentException("catalog不能为空"));
        Assert.notBlank(query.getSchema(), new SilentException("schema不能为空"));
        Assert.notBlank(query.getTable(), new SilentException("table不能为空"));
        Assert.notBlank(query.getColumn(), new SilentException("column不能为空"));
    }

    /**
     * 查询节点列表
     */
    private List<MetadataLineageNode> findNodes(List<String> nodeKeys) {
        if (nodeKeys == null || nodeKeys.isEmpty()) {
            return List.of();
        }
        return metadataLineageRepository.findNodesByKeys(nodeKeys);
    }

    /**
     * 遍历指标影响链路
     */
    private MetricTraversalResult traverseMetrics(String fieldKey, int maxDepth) {
        Map<String, MetadataLineageEdge> edgeMap = new LinkedHashMap<>();
        Set<String> metricKeys = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Queue<TraversalNode> queue = new ArrayDeque<>();
        queue.add(new TraversalNode(fieldKey, 0));
        visited.add(fieldKey);

        while (!queue.isEmpty()) {
            TraversalNode current = queue.poll();
            if (current.getDepth() >= maxDepth) {
                continue;
            }
            List<MetadataLineageEdge> edges = metadataLineageRepository.findEdgesBySourceKeys(List.of(current.getNodeKey()));
            for (MetadataLineageEdge edge : edges) {
                if (!EDGE_USES_FIELD.equals(edge.getEdgeType()) && !EDGE_DERIVES_METRIC.equals(edge.getEdgeType())) {
                    continue;
                }
                String edgeKey = edge.getSourceKey() + "->" + edge.getTargetKey() + ":" + edge.getEdgeType();
                edgeMap.putIfAbsent(edgeKey, edge);
                if (visited.add(edge.getTargetKey())) {
                    queue.add(new TraversalNode(edge.getTargetKey(), current.getDepth() + 1));
                }
                metricKeys.add(edge.getTargetKey());
            }
        }

        List<MetadataLineageNode> metrics = metadataLineageRepository.findNodesByKeys(metricKeys).stream()
                .filter(node -> NODE_METRIC.equals(node.getNodeType()))
                .toList();
        return new MetricTraversalResult(metrics, new ArrayList<>(edgeMap.values()));
    }

    /**
     * 转换节点 BO
     */
    private List<MetadataFieldLineageBO.LineageNodeBO> toNodeBOList(List<MetadataLineageNode> nodes) {
        return Optional.ofNullable(nodes).orElse(List.of()).stream()
                .map(node -> new MetadataFieldLineageBO.LineageNodeBO()
                        .setNodeKey(node.getNodeKey())
                        .setNodeType(node.getNodeType())
                        .setNodeName(node.getNodeName())
                        .setServiceName(node.getServiceName())
                        .setRefId(node.getRefId())
                        .setTableRef(node.getTableRef())
                        .setColumnName(node.getColumnName())
                        .setPropertiesJson(node.getPropertiesJson()))
                .toList();
    }

    /**
     * 转换边 BO
     */
    private List<MetadataFieldLineageBO.LineageEdgeBO> toEdgeBOList(List<MetadataLineageEdge> edges) {
        return Optional.ofNullable(edges).orElse(List.of()).stream()
                .map(edge -> new MetadataFieldLineageBO.LineageEdgeBO()
                        .setSourceKey(edge.getSourceKey())
                        .setTargetKey(edge.getTargetKey())
                        .setEdgeType(edge.getEdgeType())
                        .setServiceName(edge.getServiceName())
                        .setRefId(edge.getRefId())
                        .setPropertiesJson(edge.getPropertiesJson()))
                .toList();
    }

    /**
     * 去重边列表
     */
    private List<MetadataLineageEdge> deduplicateEdges(List<MetadataLineageEdge> edges) {
        Map<String, MetadataLineageEdge> edgeMap = new LinkedHashMap<>();
        for (MetadataLineageEdge edge : Optional.ofNullable(edges).orElse(List.of())) {
            edgeMap.putIfAbsent(edge.getSourceKey() + "->" + edge.getTargetKey() + ":" + edge.getEdgeType(), edge);
        }
        return new ArrayList<>(edgeMap.values());
    }

    /**
     * 遍历节点
     */
    private static class TraversalNode {

        /**
         * 节点唯一键
         */
        private final String nodeKey;

        /**
         * 当前深度
         */
        private final int depth;

        /**
         * 创建遍历节点
         */
        private TraversalNode(String nodeKey, int depth) {
            this.nodeKey = nodeKey;
            this.depth = depth;
        }

        /**
         * 获取节点唯一键
         */
        private String getNodeKey() {
            return nodeKey;
        }

        /**
         * 获取当前深度
         */
        private int getDepth() {
            return depth;
        }
    }

    /**
     * 指标遍历结果
     */
    private static class MetricTraversalResult {

        /**
         * 指标节点列表
         */
        private final List<MetadataLineageNode> metrics;

        /**
         * 血缘边列表
         */
        private final List<MetadataLineageEdge> edges;

        /**
         * 创建指标遍历结果
         */
        private MetricTraversalResult(List<MetadataLineageNode> metrics, List<MetadataLineageEdge> edges) {
            this.metrics = metrics;
            this.edges = edges;
        }

        /**
         * 获取指标节点列表
         */
        private List<MetadataLineageNode> metrics() {
            return metrics;
        }

        /**
         * 获取血缘边列表
         */
        private List<MetadataLineageEdge> edges() {
            return edges;
        }
    }
}
