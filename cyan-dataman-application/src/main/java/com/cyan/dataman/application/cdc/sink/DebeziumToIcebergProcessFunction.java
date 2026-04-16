package com.cyan.dataman.application.cdc.sink;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.rest.RESTCatalog;
import org.apache.iceberg.types.Types;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debezium 消息到 Iceberg Record 的转换器
 * <p>
 * 负责解析 Debezium JSON 消息并转换为 Iceberg Record。
 * 支持动态表启用/禁用（通过数据库轮询）。
 * <p>
 * 注意：此类必须可序列化（Flink 要求），所有不可序列化的字段使用 transient 懒加载。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
public class DebeziumToIcebergProcessFunction extends ProcessFunction<String, IcebergWriteRecord> {

    /**
     * 数据源名称
     */
    private final String dsName;

    /**
     * Iceberg REST Catalog URI
     */
    private final String icebergRestUri;

    /**
     * RustFS (S3) endpoint
     */
    private final String rustfsEndpoint;

    /**
     * RustFS access key
     */
    private final String rustfsAccessKey;

    /**
     * RustFS secret key
     */
    private final String rustfsSecretKey;

    /**
     * 表映射：mysqlDbName.mysqlTableName -> icebergSchema.icebergTableName
     * 例如：user.user_info -> ods.ods_user_user_info
     */
    private final Map<String, String> tableToIcebergMapping;

    /**
     * Iceberg Catalog（transient 懒加载）
     */
    private transient RESTCatalog catalog;

    /**
     * 表缓存：icebergSchema.icebergTableName -> Table
     */
    private transient Map<String, Table> tableCache;

    /**
     * 启用表集合（从数据库加载）
     */
    private transient Set<String> enabledTables;

    /**
     * 上次刷新时间
     */
    private transient long lastRefreshTime;

    /**
     * 刷新间隔（5分钟）
     */
    private static final long REFRESH_INTERVAL_MS = 5 * 60 * 1000;

    public DebeziumToIcebergProcessFunction(String dsName,
                                            Map<String, String> tableToIcebergMapping,
                                            String icebergRestUri,
                                            String rustfsEndpoint,
                                            String rustfsAccessKey,
                                            String rustfsSecretKey) {
        this.dsName = dsName;
        this.tableToIcebergMapping = tableToIcebergMapping;
        this.icebergRestUri = icebergRestUri;
        this.rustfsEndpoint = rustfsEndpoint;
        this.rustfsAccessKey = rustfsAccessKey;
        this.rustfsSecretKey = rustfsSecretKey;
    }

    @Override
    public void open(org.apache.flink.api.common.functions.OpenContext openContext) throws Exception {
        this.tableCache = new ConcurrentHashMap<>();
        this.enabledTables = ConcurrentHashMap.newKeySet();
        this.enabledTables.addAll(tableToIcebergMapping.keySet());
        this.lastRefreshTime = System.currentTimeMillis();

        // 初始化 Iceberg REST Catalog
        this.catalog = new RESTCatalog();
        Map<String, String> properties = new HashMap<>();
        properties.put("uri", icebergRestUri);
        properties.put("s3.endpoint", rustfsEndpoint);
        properties.put("s3.access-key-id", rustfsAccessKey);
        properties.put("s3.secret-access-key", rustfsSecretKey);
        properties.put("s3.path-style-access", "true");
        catalog.initialize("iceberg-catalog", properties);

        log.info("DebeziumToIcebergProcessFunction 初始化完成, dsName: {}, icebergUri: {}", dsName, icebergRestUri);
    }

    @Override
    public void close() throws Exception {
        if (catalog != null) {
            try {
                catalog.close();
            } catch (Exception e) {
                log.warn("关闭 Iceberg Catalog 失败", e);
            }
        }
    }

    @Override
    public void processElement(String debeziumJson, Context ctx, Collector<IcebergWriteRecord> out) throws Exception {
        // 解析 Debezium 消息
        DebeziumRecord record = parseDebeziumJson(debeziumJson);
        if (record == null) {
            return;
        }

        String tableKey = record.getTableKey();

        // 检查表是否启用
        if (!enabledTables.contains(tableKey)) {
            log.debug("表 {} 未启用，跳过", tableKey);
            return;
        }

        // 获取目标 Iceberg 表标识（格式：icebergSchema.icebergTableName）
        String icebergTableKey = tableToIcebergMapping.get(tableKey);
        if (icebergTableKey == null) {
            log.warn("表 {} 没有配置 Iceberg 目标表", tableKey);
            return;
        }

        // 解析 Iceberg schema 和 table name
        String[] icebergParts = icebergTableKey.split("\\.");
        String icebergSchema = icebergParts[0];
        String icebergTableName = icebergParts[1];

        try {
            // 加载 Iceberg 表
            Table table = loadTable(icebergSchema, icebergTableName);
            if (table == null) {
                log.warn("Iceberg 表不存在: {}", icebergTableKey);
                return;
            }

            // 构建 Iceberg Record
            Record icebergRecord = buildIcebergRecord(table.schema(), record);
            if (icebergRecord == null) {
                return;
            }

            // 输出写入记录
            out.collect(new IcebergWriteRecord(
                    TableIdentifier.of(icebergSchema, icebergTableName),
                    icebergRecord,
                    record.getOp()
            ));

            log.debug("转换成功: {} -> {}, op={}", tableKey, icebergTableKey, record.getOp());
        } catch (Exception e) {
            log.error("处理消息失败: tableKey={}, error={}", tableKey, e.getMessage(), e);
        }
    }

    /**
     * 加载 Iceberg 表（带缓存）
     */
    private Table loadTable(String dbName, String tableName) {
        String cacheKey = dbName + "." + tableName;
        return tableCache.computeIfAbsent(cacheKey, k -> {
            try {
                TableIdentifier tableId = TableIdentifier.of(dbName, tableName);
                if (catalog.tableExists(tableId)) {
                    return catalog.loadTable(tableId);
                }
                return null;
            } catch (Exception e) {
                log.error("加载 Iceberg 表失败: {}.{}", dbName, tableName, e);
                return null;
            }
        });
    }

    /**
     * 构建 Iceberg Record
     */
    private Record buildIcebergRecord(Schema schema, DebeziumRecord record) {
        Record icebergRecord = GenericRecord.create(schema);

        // 设置 op 字段
        icebergRecord.setField("op", record.getOp());

        // 设置数据字段
        Map<String, Object> data = record.getAfterData();
        if (data == null && record.isDelete()) {
            data = record.getBeforeData();
        }

        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();

                try {
                    // 根据字段类型转换值
                    Types.NestedField field = schema.findField(fieldName);
                    if (field != null) {
                        value = convertValue(value, field.type());
                        icebergRecord.setField(fieldName, value);
                    }
                } catch (Exception e) {
                    log.debug("设置字段 {} 失败: {}", fieldName, e.getMessage());
                }
            }
        }

        return icebergRecord;
    }

    /**
     * 根据字段类型转换值
     */
    private Object convertValue(Object value, org.apache.iceberg.types.Type type) {
        if (value == null) {
            return null;
        }

        if (type instanceof Types.StringType) {
            return value.toString();
        } else if (type instanceof Types.IntegerType) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } else if (type instanceof Types.LongType) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        } else if (type instanceof Types.DoubleType) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } else if (type instanceof Types.BooleanType) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        } else if (type instanceof Types.TimestampType) {
            // Iceberg TimestampType 需要 LocalDateTime
            if (value instanceof Number) {
                // Debezium 时间戳是毫秒，转换为 LocalDateTime
                long timestampMs = ((Number) value).longValue();
                return java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(timestampMs),
                        java.time.ZoneId.systemDefault());
            }
            return value;
        } else if (type instanceof Types.DateType) {
            // Iceberg DateType 需要 LocalDate
            if (value instanceof Number) {
                int days = ((Number) value).intValue();
                return java.time.LocalDate.ofEpochDay(days);
            }
            return value;
        }

        return value;
    }

    /**
     * 解析 Debezium JSON 消息
     */
    private DebeziumRecord parseDebeziumJson(String json) {
        try {
            DebeziumRecord record = new DebeziumRecord();

            // 提取 source 信息
            int sourceIdx = json.indexOf("\"source\":{");
            if (sourceIdx == -1) {
                return null;
            }
            int sourceStart = sourceIdx + 10;

            // 提取 db
            int dbIdx = json.indexOf("\"db\":\"", sourceStart);
            if (dbIdx != -1) {
                int dbStart = dbIdx + 6;
                int dbEnd = json.indexOf("\"", dbStart);
                record.setDbName(json.substring(dbStart, dbEnd));
            }

            // 提取 table
            int tableIdx = json.indexOf("\"table\":\"", sourceStart);
            if (tableIdx != -1) {
                int tableStart = tableIdx + 9;
                int tableEnd = json.indexOf("\"", tableStart);
                record.setTableName(json.substring(tableStart, tableEnd));
            }

            // 提取 op
            int opIdx = json.indexOf("\"op\":\"");
            if (opIdx != -1) {
                int opStart = opIdx + 6;
                int opEnd = json.indexOf("\"", opStart);
                record.setOp(json.substring(opStart, opEnd));
            }

            // 提取 ts_ms
            int tsIdx = json.indexOf("\"ts_ms\":");
            if (tsIdx != -1) {
                int tsStart = tsIdx + 8;
                int tsEnd = tsStart;
                while (tsEnd < json.length() && (Character.isDigit(json.charAt(tsEnd)) || json.charAt(tsEnd) == '.')) {
                    tsEnd++;
                }
                try {
                    record.setTimestamp(Long.parseLong(json.substring(tsStart, tsEnd)));
                } catch (NumberFormatException e) {
                    record.setTimestamp(System.currentTimeMillis());
                }
            }

            // 提取 after 数据
            record.setAfterData(extractData(json, "\"after\":"));

            // 提取 before 数据
            record.setBeforeData(extractData(json, "\"before\":"));

            return record;
        } catch (Exception e) {
            log.debug("解析 Debezium JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 提取数据字段
     */
    private Map<String, Object> extractData(String json, String fieldPrefix) {
        Map<String, Object> data = new LinkedHashMap<>();

        int afterIdx = json.indexOf(fieldPrefix + "{");
        if (afterIdx == -1) {
            return null;
        }

        int start = afterIdx + fieldPrefix.length();
        int braceCount = 1;
        int i = start + 1;

        while (i < json.length() && braceCount > 0) {
            char c = json.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            i++;
        }

        String content = json.substring(start, i);

        // 简单解析 JSON 字段
        parseJsonFields(content, data);

        return data.isEmpty() ? null : data;
    }

    /**
     * 解析 JSON 字段
     */
    private void parseJsonFields(String json, Map<String, Object> data) {
        int i = 1; // 跳过开头的 {
        while (i < json.length() - 1) {
            // 查找字段名
            int nameStart = json.indexOf("\"", i);
            if (nameStart == -1) break;
            int nameEnd = json.indexOf("\"", nameStart + 1);
            if (nameEnd == -1) break;

            String fieldName = json.substring(nameStart + 1, nameEnd);

            // 查找冒号
            int colonIdx = json.indexOf(":", nameEnd);
            if (colonIdx == -1) break;

            // 查找值
            int valueStart = colonIdx + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }

            if (valueStart >= json.length()) break;

            char firstChar = json.charAt(valueStart);
            Object value;

            if (firstChar == '"') {
                // 字符串值
                int valueEnd = json.indexOf("\"", valueStart + 1);
                if (valueEnd == -1) break;
                value = json.substring(valueStart + 1, valueEnd);
                i = valueEnd + 1;
            } else if (firstChar == 'n' && json.startsWith("null", valueStart)) {
                value = null;
                i = valueStart + 4;
            } else if (firstChar == 't' && json.startsWith("true", valueStart)) {
                value = true;
                i = valueStart + 4;
            } else if (firstChar == 'f' && json.startsWith("false", valueStart)) {
                value = false;
                i = valueStart + 5;
            } else {
                // 数字值
                int valueEnd = valueStart;
                while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '.' || json.charAt(valueEnd) == '-')) {
                    valueEnd++;
                }
                String numStr = json.substring(valueStart, valueEnd);
                if (numStr.contains(".")) {
                    value = Double.parseDouble(numStr);
                } else {
                    value = Long.parseLong(numStr);
                }
                i = valueEnd;
            }

            data.put(fieldName, value);

            // 跳过逗号
            while (i < json.length() && (json.charAt(i) == ',' || Character.isWhitespace(json.charAt(i)))) {
                i++;
            }
        }
    }
}
