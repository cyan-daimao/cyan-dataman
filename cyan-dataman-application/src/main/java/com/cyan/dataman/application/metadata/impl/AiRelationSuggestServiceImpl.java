package com.cyan.dataman.application.metadata.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.metadata.AiRelationSuggestService;
import com.cyan.dataman.application.metadata.bo.AiRelationColumnBO;
import com.cyan.dataman.application.metadata.bo.AiRelationSuggestionBO;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.TableRelation;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.domain.metadata.repository.TableRelationRepository;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.infra.gateway.DifyRelationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * AI 关联推荐服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AiRelationSuggestServiceImpl implements AiRelationSuggestService {

    private static final int DEFAULT_MAX_CANDIDATES = 8;
    private static final int MAX_SAME_SCHEMA_TABLES = 50;
    private static final int MAX_GLOBAL_TABLES = 50;
    private static final Set<String> JOIN_TYPES = Set.of("LEFT", "INNER", "RIGHT");

    private final MetadataTableRepository metadataTableRepository;
    private final TableRelationRepository tableRelationRepository;
    private final DifyRelationGateway difyRelationGateway;

    /**
     * 推荐表关联关系
     */
    @Override
    public List<AiRelationSuggestionBO> suggest(String catalog, String schema, String table, Integer maxCandidates) {
        int limit = normalizeLimit(maxCandidates);
        MetadataTable current = findTable(catalog, schema, table);
        if (current == null) {
            throw new SilentException("当前表不存在: " + catalog + "." + schema + "." + table);
        }
        if (!difyRelationGateway.available()) {
            return List.of();
        }

        Map<String, MetadataTable> tablePool = buildCandidatePool(current);
        if (tablePool.size() <= 1) {
            return List.of();
        }

        String prompt = buildPrompt(current, tablePool.values().stream()
                .filter(t -> !tableKey(current).equals(tableKey(t)))
                .toList(), limit);
        String answer = difyRelationGateway.suggestRelations(prompt);
        JSONArray suggestions = parseSuggestions(answer);
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }

        Set<String> existingRelationKeys = buildExistingRelationKeys(catalog, schema, table);
        List<AiRelationSuggestionBO> result = new ArrayList<>();
        Set<String> acceptedKeys = new LinkedHashSet<>();
        for (int i = 0; i < suggestions.size() && result.size() < limit; i++) {
            JSONObject item = suggestions.getJSONObject(i);
            AiRelationSuggestionBO suggestion = toSuggestion(item, current, tablePool, existingRelationKeys);
            if (suggestion == null) {
                continue;
            }
            String relationKey = relationKey(suggestion.getSourceCatalog(), suggestion.getSourceSchema(),
                    suggestion.getSourceTable(), suggestion.getSourceColumn(), suggestion.getTargetCatalog(),
                    suggestion.getTargetSchema(), suggestion.getTargetTable(), suggestion.getTargetColumn());
            if (acceptedKeys.add(relationKey)) {
                result.add(suggestion);
            }
        }
        return result;
    }

    /**
     * 标准化最大候选数
     */
    private int normalizeLimit(Integer maxCandidates) {
        if (maxCandidates == null || maxCandidates <= 0) {
            return DEFAULT_MAX_CANDIDATES;
        }
        return Math.min(maxCandidates, DEFAULT_MAX_CANDIDATES);
    }

    /**
     * 查询指定元数据表
     */
    private MetadataTable findTable(String catalog, String schema, String table) {
        MetadataTable one = metadataTableRepository.findOne(new MetadataTableOneQuery()
                .setCatalog(catalog)
                .setSchema(schema)
                .setName(table));
        if (one == null) {
            return null;
        }
        return metadataTableRepository.findById(one.getId());
    }

    /**
     * 构建候选表池
     */
    private Map<String, MetadataTable> buildCandidatePool(MetadataTable current) {
        Map<String, MetadataTable> pool = new LinkedHashMap<>();
        pool.put(tableKey(current), current);

        List<MetadataTable> sameSchemaTables = metadataTableRepository.list(new MetadataTableListQuery()
                .setCatalog(current.getTable().getCatalog())
                .setSchema(current.getTable().getSchema()));
        addTables(pool, sameSchemaTables, MAX_SAME_SCHEMA_TABLES);

        Set<String> keywords = extractKeywords(current);
        int globalAdded = 0;
        for (String keyword : keywords) {
            if (globalAdded >= MAX_GLOBAL_TABLES) {
                break;
            }
            List<MetadataTable> tables = metadataTableRepository.list(new MetadataTableListQuery().setContent(keyword));
            for (MetadataTable table : Optional.ofNullable(tables).orElse(List.of())) {
                if (globalAdded >= MAX_GLOBAL_TABLES) {
                    break;
                }
                String key = tableKey(table);
                if (!pool.containsKey(key)) {
                    pool.put(key, ensureColumns(table));
                    globalAdded++;
                }
            }
        }
        return pool;
    }

    /**
     * 添加候选表
     */
    private void addTables(Map<String, MetadataTable> pool, List<MetadataTable> tables, int limit) {
        int count = 0;
        for (MetadataTable table : Optional.ofNullable(tables).orElse(List.of())) {
            if (count >= limit) {
                break;
            }
            String key = tableKey(table);
            if (!pool.containsKey(key)) {
                pool.put(key, ensureColumns(table));
                count++;
            }
        }
    }

    /**
     * 补齐字段列表
     */
    private MetadataTable ensureColumns(MetadataTable table) {
        if (table == null || table.getId() == null) {
            return table;
        }
        if (table.getTable() != null && table.getTable().getColumns() != null) {
            return table;
        }
        return metadataTableRepository.findById(table.getId());
    }

    /**
     * 提取候选搜索关键词
     */
    private Set<String> extractKeywords(MetadataTable current) {
        Set<String> keywords = new LinkedHashSet<>();
        addKeywordParts(keywords, current.getName());
        addKeywordParts(keywords, current.getComment());
        Optional.ofNullable(current.getTable())
                .map(t -> t.getColumns())
                .orElse(List.of())
                .stream()
                .limit(12)
                .forEach(col -> {
                    addKeywordParts(keywords, col.getName());
                    addKeywordParts(keywords, col.getComment());
                });
        return keywords;
    }

    /**
     * 添加关键词片段
     */
    private void addKeywordParts(Set<String> keywords, String text) {
        if (text == null || text.isBlank() || keywords.size() >= 12) {
            return;
        }
        String normalized = text.replaceAll("[^\\p{IsHan}A-Za-z0-9]+", " ");
        for (String part : normalized.split("\\s+")) {
            if (part.length() >= 2) {
                keywords.add(part);
            }
            if (keywords.size() >= 12) {
                return;
            }
        }
    }

    /**
     * 构建 Dify 提示词
     */
    private String buildPrompt(MetadataTable current, List<MetadataTable> candidates, int limit) {
        JSONObject payload = new JSONObject();
        payload.put("currentTable", toPromptTable(current));
        payload.put("candidateTables", candidates.stream().map(this::toPromptTable).toList());
        payload.put("maxCandidates", limit);
        return """
                你是数据仓库建模专家。请根据当前表和候选表的元数据，推测可能的表关联关系。
                只返回严格 JSON，不要返回 Markdown，不要解释额外文本。
                JSON 格式：
                {"suggestions":[{"sourceCatalog":"","sourceSchema":"","sourceTable":"","sourceColumn":"","targetCatalog":"","targetSchema":"","targetTable":"","targetColumn":"","joinType":"LEFT","confidence":0.8,"reason":"推荐理由"}]}
                约束：
                1. source 或 target 其中一侧必须是 currentTable。
                2. 只能使用输入元数据中存在的表和字段。
                3. joinType 只能是 LEFT、INNER、RIGHT。
                4. 优先推荐字段名相同、字段注释语义相近、id/code/key 后缀匹配的关系。
                5. 不确定时降低 confidence，不要编造字段。
                元数据如下：
                """ + JSON.toJSONString(payload);
    }

    /**
     * 转换为提示词表结构
     */
    private JSONObject toPromptTable(MetadataTable table) {
        JSONObject object = new JSONObject();
        object.put("catalog", table.getTable().getCatalog());
        object.put("schema", table.getTable().getSchema());
        object.put("table", table.getName());
        object.put("comment", table.getComment());
        object.put("columns", Optional.ofNullable(table.getTable().getColumns()).orElse(List.of()).stream()
                .map(this::toPromptColumn)
                .toList());
        return object;
    }

    /**
     * 转换为提示词字段结构
     */
    private JSONObject toPromptColumn(ColumnValObj column) {
        JSONObject object = new JSONObject();
        object.put("name", column.getName());
        object.put("type", column.getType());
        object.put("comment", column.getComment());
        return object;
    }

    /**
     * 解析 Dify 推荐结果
     */
    private JSONArray parseSuggestions(String answer) {
        if (answer == null || answer.isBlank()) {
            return null;
        }
        String jsonText = extractJson(answer);
        if (jsonText == null || jsonText.isBlank()) {
            return null;
        }
        try {
            if (jsonText.startsWith("[")) {
                return JSON.parseArray(jsonText);
            }
            JSONObject object = JSON.parseObject(jsonText);
            JSONArray suggestions = object.getJSONArray("suggestions");
            if (suggestions == null) {
                suggestions = object.getJSONArray("relations");
            }
            if (suggestions == null) {
                suggestions = object.getJSONArray("recommendations");
            }
            return suggestions;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从回复中提取 JSON 片段
     */
    private String extractJson(String answer) {
        String text = answer.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return text.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');
        if (objectStart < 0 && arrayStart < 0) {
            return null;
        }
        if (arrayStart >= 0 && (objectStart < 0 || arrayStart < objectStart)) {
            int arrayEnd = text.lastIndexOf(']');
            return arrayEnd > arrayStart ? text.substring(arrayStart, arrayEnd + 1).trim() : null;
        }
        int objectEnd = text.lastIndexOf('}');
        return objectEnd > objectStart ? text.substring(objectStart, objectEnd + 1).trim() : null;
    }

    /**
     * 转换并校验单条推荐
     */
    private AiRelationSuggestionBO toSuggestion(JSONObject item, MetadataTable current,
                                                Map<String, MetadataTable> tablePool,
                                                Set<String> existingRelationKeys) {
        if (item == null) {
            return null;
        }
        String sourceCatalog = readString(item, "sourceCatalog", "source_catalog");
        String sourceSchema = readString(item, "sourceSchema", "source_schema");
        String sourceTable = readString(item, "sourceTable", "source_table");
        String sourceColumn = readString(item, "sourceColumn", "source_column");
        String targetCatalog = readString(item, "targetCatalog", "target_catalog");
        String targetSchema = readString(item, "targetSchema", "target_schema");
        String targetTable = readString(item, "targetTable", "target_table");
        String targetColumn = readString(item, "targetColumn", "target_column");
        String joinType = Optional.ofNullable(readString(item, "joinType", "join_type"))
                .orElse("LEFT")
                .toUpperCase(Locale.ROOT);
        if (!JOIN_TYPES.contains(joinType)) {
            return null;
        }
        String sourceKey = tableKey(sourceCatalog, sourceSchema, sourceTable);
        String targetKey = tableKey(targetCatalog, targetSchema, targetTable);
        MetadataTable source = tablePool.get(sourceKey);
        MetadataTable target = tablePool.get(targetKey);
        if (source == null || target == null) {
            return null;
        }
        String currentKey = tableKey(current);
        if (!currentKey.equals(sourceKey) && !currentKey.equals(targetKey)) {
            return null;
        }
        if (!hasColumn(source, sourceColumn) || !hasColumn(target, targetColumn)) {
            return null;
        }
        String directKey = relationKey(sourceCatalog, sourceSchema, sourceTable, sourceColumn,
                targetCatalog, targetSchema, targetTable, targetColumn);
        String reverseKey = relationKey(targetCatalog, targetSchema, targetTable, targetColumn,
                sourceCatalog, sourceSchema, sourceTable, sourceColumn);
        if (existingRelationKeys.contains(directKey) || existingRelationKeys.contains(reverseKey)) {
            return null;
        }
        String reason = Optional.ofNullable(readString(item, "reason")).orElse("AI 根据表字段语义推测可关联");
        return new AiRelationSuggestionBO()
                .setSourceCatalog(sourceCatalog)
                .setSourceSchema(sourceSchema)
                .setSourceTable(sourceTable)
                .setSourceTableComment(source.getComment())
                .setSourceColumn(sourceColumn)
                .setSourceColumns(toColumnBOList(source))
                .setTargetCatalog(targetCatalog)
                .setTargetSchema(targetSchema)
                .setTargetTable(targetTable)
                .setTargetTableComment(target.getComment())
                .setTargetColumn(targetColumn)
                .setTargetColumns(toColumnBOList(target))
                .setJoinType(joinType)
                .setConfidence(normalizeConfidence(item.getDouble("confidence")))
                .setReason(reason)
                .setDescription("[AI推荐] " + reason);
    }

    /**
     * 读取字符串字段
     */
    private String readString(JSONObject object, String... names) {
        for (String name : names) {
            String value = object.getString(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 标准化置信度
     */
    private Double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return 0.5D;
        }
        if (confidence > 1D) {
            confidence = confidence / 100D;
        }
        return Math.max(0D, Math.min(1D, confidence));
    }

    /**
     * 判断字段是否存在
     */
    private boolean hasColumn(MetadataTable table, String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return false;
        }
        return Optional.ofNullable(table.getTable())
                .map(t -> t.getColumns())
                .orElse(List.of())
                .stream()
                .anyMatch(c -> columnName.equals(c.getName()));
    }

    /**
     * 转换字段选项
     */
    private List<AiRelationColumnBO> toColumnBOList(MetadataTable table) {
        return Optional.ofNullable(table.getTable())
                .map(t -> t.getColumns())
                .orElse(List.of())
                .stream()
                .map(c -> new AiRelationColumnBO()
                        .setName(c.getName())
                        .setType(c.getType())
                        .setComment(c.getComment()))
                .toList();
    }

    /**
     * 构建已有关系键
     */
    private Set<String> buildExistingRelationKeys(String catalog, String schema, String table) {
        Set<String> keys = new LinkedHashSet<>();
        Optional.ofNullable(tableRelationRepository.listBySource(catalog, schema, table)).orElse(List.of())
                .forEach(relation -> keys.add(relationKey(relation)));
        Optional.ofNullable(tableRelationRepository.listByTarget(catalog, schema, table)).orElse(List.of())
                .forEach(relation -> keys.add(relationKey(relation)));
        return keys;
    }

    /**
     * 构建关系键
     */
    private String relationKey(TableRelation relation) {
        return relationKey(relation.getSourceCatalog(), relation.getSourceSchema(), relation.getSourceTable(),
                relation.getSourceColumn(), relation.getTargetCatalog(), relation.getTargetSchema(),
                relation.getTargetTable(), relation.getTargetColumn());
    }

    /**
     * 构建关系键
     */
    private String relationKey(String sourceCatalog, String sourceSchema, String sourceTable, String sourceColumn,
                               String targetCatalog, String targetSchema, String targetTable, String targetColumn) {
        return tableKey(sourceCatalog, sourceSchema, sourceTable) + "." + sourceColumn
                + "->" + tableKey(targetCatalog, targetSchema, targetTable) + "." + targetColumn;
    }

    /**
     * 构建表键
     */
    private String tableKey(MetadataTable table) {
        return tableKey(table.getTable().getCatalog(), table.getTable().getSchema(), table.getName());
    }

    /**
     * 构建表键
     */
    private String tableKey(String catalog, String schema, String table) {
        return catalog + "." + schema + "." + table;
    }
}
