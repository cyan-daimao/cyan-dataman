package com.cyan.dataman.application.metadata.impl;

import com.alibaba.fastjson2.JSONObject;
import com.cyan.dataman.application.metadata.RelationVectorIndexService;
import com.cyan.dataman.application.metadata.bo.RelationVectorIndexResultBO;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.TableValObj;
import com.cyan.dataman.infra.gateway.RelationEmbeddingGateway;
import com.cyan.dataman.infra.gateway.RelationVectorGateway;
import com.cyan.dataman.infra.gateway.model.RelationVectorDocument;
import com.cyan.dataman.infra.gateway.model.RelationVectorSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 关联推荐向量索引服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RelationVectorIndexServiceImpl implements RelationVectorIndexService {

    private final MetadataTableRepository metadataTableRepository;
    private final RelationEmbeddingGateway relationEmbeddingGateway;
    private final RelationVectorGateway relationVectorGateway;

    /**
     * 是否可用
     */
    @Override
    public boolean available() {
        return relationEmbeddingGateway.available() && relationVectorGateway.available();
    }

    /**
     * 重建所有表索引
     */
    @Override
    public RelationVectorIndexResultBO rebuildAll() {
        List<MetadataTable> tables = Optional.ofNullable(metadataTableRepository.list(new MetadataTableListQuery()))
                .orElse(List.of());
        int success = 0;
        int failed = 0;
        for (MetadataTable table : tables) {
            try {
                MetadataTable fullTable = metadataTableRepository.findById(table.getId());
                upsert(fullTable);
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("重建元数据表向量索引失败, tableId: {}", table.getId(), e);
            }
        }
        return new RelationVectorIndexResultBO()
                .setTotalCount(tables.size())
                .setSuccessCount(success)
                .setFailedCount(failed);
    }

    /**
     * 重建单表索引
     */
    @Override
    public RelationVectorIndexResultBO rebuildOne(String tableId) {
        MetadataTable table = metadataTableRepository.findById(tableId);
        if (table == null) {
            return result(1, 0, 1);
        }
        try {
            upsert(table);
            return result(1, 1, 0);
        } catch (Exception e) {
            log.warn("重建元数据表向量索引失败, tableId: {}", tableId, e);
            return result(1, 0, 1);
        }
    }

    /**
     * 写入单表索引
     */
    @Override
    public void upsert(MetadataTable table) {
        if (!available() || table == null || table.getTable() == null) {
            return;
        }
        List<Double> vector = relationEmbeddingGateway.embed(buildEmbeddingText(table));
        if (vector == null || vector.isEmpty()) {
            return;
        }
        int dimension = relationEmbeddingGateway.dimension() > 0 ? relationEmbeddingGateway.dimension() : vector.size();
        relationVectorGateway.ensureCollection(dimension);
        relationVectorGateway.upsert(new RelationVectorDocument()
                .setPointId(pointId(table))
                .setVector(vector)
                .setPayload(buildPayload(table)));
    }

    /**
     * 删除单表索引
     */
    @Override
    public void delete(MetadataTable table) {
        if (!available() || table == null || table.getTable() == null) {
            return;
        }
        relationVectorGateway.delete(pointId(table));
    }

    /**
     * 删除单表索引
     */
    @Override
    public RelationVectorIndexResultBO deleteOne(String tableId) {
        MetadataTable table = metadataTableRepository.findById(tableId);
        if (table == null) {
            return result(1, 0, 1);
        }
        try {
            delete(table);
            return result(1, 1, 0);
        } catch (Exception e) {
            log.warn("删除元数据表向量索引失败, tableId: {}", tableId, e);
            return result(1, 0, 1);
        }
    }

    /**
     * 搜索相似表
     */
    @Override
    public List<MetadataTable> searchSimilarTables(MetadataTable current) {
        if (!available() || current == null || current.getTable() == null) {
            return List.of();
        }
        List<Double> vector = relationEmbeddingGateway.embed(buildEmbeddingText(current));
        if (vector == null || vector.isEmpty()) {
            return List.of();
        }
        Map<String, MetadataTable> result = new LinkedHashMap<>();
        String currentKey = tableKey(current);
        List<RelationVectorSearchResult> searchResults = relationVectorGateway.search(vector, relationVectorGateway.topK());
        for (RelationVectorSearchResult item : searchResults) {
            MetadataTable table = findTable(item);
            if (table == null || table.getTable() == null || currentKey.equals(tableKey(table))) {
                continue;
            }
            result.putIfAbsent(tableKey(table), table);
        }
        return result.values().stream().toList();
    }

    /**
     * 根据搜索结果查询元数据表
     */
    private MetadataTable findTable(RelationVectorSearchResult item) {
        if (item == null) {
            return null;
        }
        if (item.getTableId() != null && !item.getTableId().isBlank()) {
            MetadataTable table = metadataTableRepository.findById(item.getTableId());
            if (table != null) {
                return table;
            }
        }
        MetadataTable table = metadataTableRepository.findOne(new MetadataTableOneQuery()
                .setCatalog(item.getCatalog())
                .setSchema(item.getSchema())
                .setName(item.getTable()));
        return table == null ? null : metadataTableRepository.findById(table.getId());
    }

    /**
     * 构建 Embedding 文本
     */
    private String buildEmbeddingText(MetadataTable table) {
        StringBuilder builder = new StringBuilder();
        builder.append("table: ").append(tableKey(table)).append('\n');
        builder.append("name: ").append(table.getName()).append('\n');
        builder.append("comment: ").append(nullToEmpty(table.getComment())).append('\n');
        builder.append("columns:\n");
        Optional.ofNullable(table.getTable())
                .map(TableValObj::getColumns)
                .orElse(List.of())
                .forEach(column -> appendColumn(builder, column));
        return builder.toString();
    }

    /**
     * 追加字段文本
     */
    private void appendColumn(StringBuilder builder, ColumnValObj column) {
        builder.append("- ")
                .append(nullToEmpty(column.getName()))
                .append(" ")
                .append(nullToEmpty(column.getType()))
                .append(" ")
                .append(nullToEmpty(column.getComment()))
                .append('\n');
    }

    /**
     * 构建 Qdrant 载荷
     */
    private JSONObject buildPayload(MetadataTable table) {
        JSONObject payload = new JSONObject();
        payload.put("catalog", table.getTable().getCatalog());
        payload.put("schema", table.getTable().getSchema());
        payload.put("table", table.getName());
        payload.put("tableId", table.getId());
        payload.put("comment", table.getComment());
        payload.put("updatedAt", Optional.ofNullable(table.getUpdatedAt()).map(LocalDateTime::toString).orElse(null));
        return payload;
    }

    /**
     * 构建结果
     */
    private RelationVectorIndexResultBO result(int total, int success, int failed) {
        return new RelationVectorIndexResultBO()
                .setTotalCount(total)
                .setSuccessCount(success)
                .setFailedCount(failed);
    }

    /**
     * 构建点ID
     */
    private String pointId(MetadataTable table) {
        return UUID.nameUUIDFromBytes(tableKey(table).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * 构建表键
     */
    private String tableKey(MetadataTable table) {
        return table.getTable().getCatalog() + "." + table.getTable().getSchema() + "." + table.getName();
    }

    /**
     * 空值转空字符串
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
