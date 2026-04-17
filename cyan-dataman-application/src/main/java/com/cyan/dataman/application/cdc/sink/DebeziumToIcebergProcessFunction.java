package com.cyan.dataman.application.cdc.sink;

import com.cyan.arch.common.util.JSON;
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

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Debezium 消息到 Iceberg Record 的转换器（动态路由版）
 * <p>
 * 定时从数据库查询该数据源下所有 enabled 的 CDC 配置，动态更新路由映射。
 * 支持不停作业的情况下增减同步表。
 * <p>
 * 注意：此类必须可序列化（Flink 要求），所有不可序列化的字段使用 transient 懒加载。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
public class DebeziumToIcebergProcessFunction extends ProcessFunction<String, IcebergWriteRecord> {

    /**
     * 数据源名称（用于查询 CDC 配置和构造 topic pattern）
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
     * JDBC 连接 URL（用于定时查询 CDC 配置）
     */
    private final String jdbcUrl;

    /**
     * JDBC 用户名
     */
    private final String jdbcUsername;

    /**
     * JDBC 密码
     */
    private final String jdbcPassword;

    /**
     * Iceberg Catalog（transient 懒加载）
     */
    private transient RESTCatalog catalog;

    /**
     * 表缓存：icebergSchema.icebergTableName -> Table
     */
    private transient Map<String, Table> tableCache;

    /**
     * 当前路由映射：dbName.tableName -> icebergSchema.icebergTableName
     * 由定时任务从数据库刷新
     */
    private transient volatile Map<String, String> tableToIcebergMapping;

    /**
     * 当前启用的表集合：dbName.tableName
     * 由定时任务从数据库刷新
     */
    private transient volatile Map<String, String> enabledTables;

    /**
     * 定时刷新线程
     */
    private transient ScheduledExecutorService scheduler;

    /**
     * 刷新间隔（秒）
     */
    private static final long REFRESH_INTERVAL_SECONDS = 30;

    public DebeziumToIcebergProcessFunction(String dsName,
                                            String icebergRestUri,
                                            String rustfsEndpoint,
                                            String rustfsAccessKey,
                                            String rustfsSecretKey,
                                            String jdbcUrl,
                                            String jdbcUsername,
                                            String jdbcPassword) {
        this.dsName = dsName;
        this.icebergRestUri = icebergRestUri;
        this.rustfsEndpoint = rustfsEndpoint;
        this.rustfsAccessKey = rustfsAccessKey;
        this.rustfsSecretKey = rustfsSecretKey;
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    @Override
    public void open(org.apache.flink.api.common.functions.OpenContext openContext) throws Exception {
        this.tableCache = new ConcurrentHashMap<>();
        this.tableToIcebergMapping = new HashMap<>();
        this.enabledTables = new HashMap<>();

        // 初始化 Iceberg REST Catalog
        this.catalog = new RESTCatalog();
        Map<String, String> properties = new HashMap<>();
        properties.put("uri", icebergRestUri);
        properties.put("s3.endpoint", rustfsEndpoint);
        properties.put("s3.access-key-id", rustfsAccessKey);
        properties.put("s3.secret-access-key", rustfsSecretKey);
        properties.put("s3.path-style-access", "true");
        catalog.initialize("iceberg-catalog", properties);

        // 首次加载配置
        refreshConfig();

        // 启动定时刷新
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cdc-config-refresh-" + dsName);
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::refreshConfig, REFRESH_INTERVAL_SECONDS, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("DebeziumToIcebergProcessFunction 初始化完成, dsName: {}, 初始映射表数: {}", dsName, tableToIcebergMapping.size());
    }

    @Override
    public void close() throws Exception {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
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

        // 使用最新的映射快照（volatile 保证可见性）
        Map<String, String> currentMapping = this.tableToIcebergMapping;
        Map<String, String> currentEnabled = this.enabledTables;

        // 检查表是否启用
        if (!currentEnabled.containsKey(tableKey)) {
            return;
        }

        // 获取目标 Iceberg 表标识
        String icebergTableKey = currentMapping.get(tableKey);
        if (icebergTableKey == null) {
            log.debug("表 {} 无 Iceberg 映射，跳过", tableKey);
            return;
        }

        String[] icebergParts = icebergTableKey.split("\\.");
        String icebergSchema = icebergParts[0];
        String icebergTableName = icebergParts[1];

        try {
            Table table = loadTable(icebergSchema, icebergTableName);
            if (table == null) {
                log.warn("Iceberg 表不存在: {}", icebergTableKey);
                return;
            }

            Record icebergRecord = buildIcebergRecord(table.schema(), record);
            if (icebergRecord == null) {
                return;
            }

            out.collect(new IcebergWriteRecord(
                    TableIdentifier.of(icebergSchema, icebergTableName),
                    icebergRecord,
                    record.getOp()));

            log.debug("转换成功: {} -> {}, op={}", tableKey, icebergTableKey, record.getOp());
        } catch (Exception e) {
            log.error("处理消息失败: tableKey={}, error={}", tableKey, e.getMessage(), e);
        }
    }

    /**
     * 从数据库刷新 CDC 配置
     * <p>
     * 查询 cdc_config 表获取该数据源下所有 enabled 的 Flink CDC 配置，
     * 并从 metadata_table 查询 Iceberg schema 信息构建完整映射。
     */
    private void refreshConfig() {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword)) {
            Map<String, String> newMapping = new HashMap<>();
            Map<String, String> newEnabled = new HashMap<>();

            // 查询所有 enabled 的 Flink CDC 配置
            String sql = "SELECT db_name, table_name, iceberg_table_name FROM cdc_config " +
                    "WHERE ds_name = ? AND enabled = 1 AND sync_tool = 'FLINK' AND deleted_at IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, dsName);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String dbName = rs.getString("db_name");
                    String tableName = rs.getString("table_name");
                    String icebergTableName = rs.getString("iceberg_table_name");

                    String mysqlTableKey = dbName + "." + tableName;
                    newEnabled.put(mysqlTableKey, icebergTableName);
                }
            }

            // 从 metadata_table 查询 Iceberg schema 构建完整映射
            for (Map.Entry<String, String> entry : newEnabled.entrySet()) {
                String mysqlTableKey = entry.getKey();
                String icebergTableName = entry.getValue();

                String tableName;
                if (icebergTableName.contains(".")) {
                    String[] parts = icebergTableName.split("\\.");
                    tableName = parts[parts.length - 1];
                } else {
                    tableName = icebergTableName;
                }

                String schemaSql = "SELECT t.schema FROM metadata_table t WHERE t.name = ? AND t.deleted_at IS NULL LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(schemaSql)) {
                    ps.setString(1, tableName);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String icebergSchema = rs.getString("schema");
                        if (icebergSchema != null) {
                            newMapping.put(mysqlTableKey, icebergSchema + "." + tableName);
                        }
                    }
                }
            }

            // 原子替换
            this.tableToIcebergMapping = newMapping;
            this.enabledTables = newEnabled;

            // 清除不再需要的 Iceberg 表缓存
            tableCache.keySet().retainAll(newMapping.values());

            log.info("CDC 配置刷新完成, dsName={}, enabled表数={}, mapping={}", dsName, newEnabled.size(), newMapping);
        } catch (Exception e) {
            log.error("刷新 CDC 配置失败, dsName={}, error={}", dsName, e.getMessage(), e);
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

        icebergRecord.setField("op", record.getOp());

        Map<String, Object> data = record.getAfterData();
        if (data == null && record.isDelete()) {
            data = record.getBeforeData();
        }

        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                try {
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
            if (value instanceof Number) {
                long timestampMs = ((Number) value).longValue();
                return java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(timestampMs),
                        java.time.ZoneId.systemDefault());
            }
            return value;
        } else if (type instanceof Types.DateType) {
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
     * <p>
     * Debezium 消息格式: {"schema": {...}, "payload": {"source": {...}, "op": ..., ...}}
     */
    private DebeziumRecord parseDebeziumJson(String json) {
        try {
            DebeziumEnvelope envelope = JSON.parseObject(json, DebeziumEnvelope.class);
            if (envelope != null && envelope.getPayload() != null) {
                return DebeziumRecord.from(envelope.getPayload());
            }

            DebeziumPayload payload = JSON.parseObject(json, DebeziumPayload.class);
            return DebeziumRecord.from(payload);
        } catch (Exception e) {
            log.debug("解析 Debezium JSON 失败: {}", e.getMessage());
            return null;
        }
    }
}
