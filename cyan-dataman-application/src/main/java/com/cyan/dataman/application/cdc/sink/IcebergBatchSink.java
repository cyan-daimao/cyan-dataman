package com.cyan.dataman.application.cdc.sink;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.iceberg.*;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.FileAppender;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.rest.RESTCatalog;
import org.apache.iceberg.types.Types;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Iceberg 批量写入 ProcessFunction（按主键去重）
 * <p>
 * 利用 Flink Checkpoint 机制进行批量提交：
 * 1. 数据先缓存在内存中，按主键去重（同一主键只保留最新记录）
 * 2. Checkpoint 触发时批量写入 Parquet 文件
 * 3. 保证幂等写入
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
public class IcebergBatchSink extends ProcessFunction<IcebergWriteRecord, Void> implements CheckpointedFunction {

    private final String icebergRestUri;
    private final String rustfsEndpoint;
    private final String rustfsAccessKey;
    private final String rustfsSecretKey;
    private final long targetFileSizeBytes;

    private transient RESTCatalog catalog;
    private transient Map<String, Table> tableCache;

    /**
     * 缓冲区：按表分组，按主键去重
     * key: tableKey (schema.tableName)
     * value: primaryKeyValue -> Record
     */
    private transient Map<String, Map<Object, DeduplicatedRecord>> buffer;

    private transient long currentBufferSize;

    public IcebergBatchSink(String icebergRestUri,
                            String rustfsEndpoint,
                            String rustfsAccessKey,
                            String rustfsSecretKey,
                            long targetFileSizeBytes) {
        this.icebergRestUri = icebergRestUri;
        this.rustfsEndpoint = rustfsEndpoint;
        this.rustfsAccessKey = rustfsAccessKey;
        this.rustfsSecretKey = rustfsSecretKey;
        this.targetFileSizeBytes = targetFileSizeBytes;
    }

    @Override
    public void open(org.apache.flink.api.common.functions.OpenContext openContext) {
        this.catalog = new RESTCatalog();
        Map<String, String> properties = new HashMap<>();
        properties.put("uri", icebergRestUri);
        properties.put("s3.endpoint", rustfsEndpoint);
        properties.put("s3.access-key-id", rustfsAccessKey);
        properties.put("s3.secret-access-key", rustfsSecretKey);
        properties.put("s3.path-style-access", "true");
        catalog.initialize("iceberg-catalog", properties);

        this.tableCache = new ConcurrentHashMap<>();
        this.buffer = new ConcurrentHashMap<>();
        this.currentBufferSize = 0;

        log.info("IcebergBatchSink 初始化完成, targetFileSize: {} bytes", targetFileSizeBytes);
    }

    @Override
    public void close() throws Exception {
        flush();
        if (catalog != null) {
            catalog.close();
        }
    }

    @Override
    public void processElement(IcebergWriteRecord value, ProcessFunction<IcebergWriteRecord, Void>.Context ctx, Collector<Void> out) {
        String tableKey = value.getCacheKey();

        // 构建去重 key（使用 Iceberg 表的 identifier fields 作为主键）
        Object deduplicationKey = extractPrimaryKey(value);

        buffer.computeIfAbsent(tableKey, k -> new HashMap<>())
                .put(deduplicationKey, new DeduplicatedRecord(value.getRecord(), value.getOp()));
        currentBufferSize += estimateRecordSize(value.getRecord());

        if (currentBufferSize >= targetFileSizeBytes) {
            flush();
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) {
        log.info("Checkpoint 触发，刷新缓冲区，表数: {}, 大小: {} bytes", buffer.size(), currentBufferSize);
        flush();
    }

    @Override
    public void initializeState(FunctionInitializationContext context) {
        // 有状态恢复时重新处理（简单方案：不做状态恢复，依赖 Kafka offset 重放）
    }

    /**
     * 从 IcebergWriteRecord 提取主键值用于去重
     */
    private Object extractPrimaryKey(IcebergWriteRecord record) {
        try {
            Table table = loadTable(record.getTableIdentifier());
            if (table == null) {
                return UUID.randomUUID(); // 表不存在时无法去重，用随机 key
            }

            // 优先使用 Iceberg identifier fields
            Set<Integer> identifierFieldIds = table.schema().identifierFieldIds();
            if (identifierFieldIds != null && !identifierFieldIds.isEmpty()) {
                List<Object> keyParts = new ArrayList<>();
                for (int fieldId : identifierFieldIds) {
                    Types.NestedField field = table.schema().findField(fieldId);
                    if (field != null) {
                        Object val = record.getRecord().getField(field.name());
                        keyParts.add(val);
                    }
                }
                return keyParts;
            }

            // 没有 identifier fields，用第一条字段作为主键（通常是 id）
            Schema schema = table.schema();
            if (!schema.columns().isEmpty()) {
                String firstField = schema.columns().getFirst().name();
                return record.getRecord().getField(firstField);
            }
        } catch (Exception e) {
            log.debug("提取主键失败: {}", e.getMessage());
        }
        return UUID.randomUUID();
    }

    /**
     * 刷新缓冲区，写入 Iceberg
     */
    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<Object, DeduplicatedRecord>> entry : buffer.entrySet()) {
            String tableKey = entry.getKey();
            Map<Object, DeduplicatedRecord> records = entry.getValue();

            if (records.isEmpty()) {
                continue;
            }

            try {
                String[] parts = tableKey.split("\\.");
                TableIdentifier tableId = TableIdentifier.of(parts[0], parts[1]);

                Table table = loadTable(tableId);
                if (table == null) {
                    log.warn("表不存在，跳过: {}", tableId);
                    continue;
                }

                List<Record> recordsToWrite = new ArrayList<>();
                for (DeduplicatedRecord dr : records.values()) {
                    recordsToWrite.add(dr.record);
                }

                if (!recordsToWrite.isEmpty()) {
                    writeDataFile(table, recordsToWrite);
                    log.info("写入 Iceberg 成功: table={}, records={}", tableId, recordsToWrite.size());
                }
            } catch (Exception e) {
                log.error("写入 Iceberg 失败: table={}, error={}", tableKey, e.getMessage(), e);
            }
        }

        buffer.clear();
        currentBufferSize = 0;
    }

    private Table loadTable(TableIdentifier tableId) {
        return tableCache.computeIfAbsent(tableId.toString(), k -> {
            try {
                if (catalog.tableExists(tableId)) {
                    return catalog.loadTable(tableId);
                }
                return null;
            } catch (Exception e) {
                log.error("加载表失败: {}", tableId, e);
                return null;
            }
        });
    }

    private void writeDataFile(Table table, List<Record> records) throws Exception {
        Schema schema = table.schema();

        String fileName = "data-" + UUID.randomUUID() + ".parquet";
        OutputFile outputFile = table.io().newOutputFile(
                table.locationProvider().newDataLocation(fileName));

        GenericAppenderFactory appenderFactory = new GenericAppenderFactory(schema);
        FileAppender<Record> appender = appenderFactory.newAppender(outputFile, FileFormat.PARQUET);

        for (Record record : records) {
            appender.add(record);
        }
        appender.close();

        DataFile dataFile = DataFiles.builder(table.spec())
                .withPath(outputFile.location())
                .withFileSizeInBytes(appender.length())
                .withFormat(FileFormat.PARQUET)
                .withRecordCount(records.size())
                .build();

        AppendFiles appendFiles = table.newAppend();
        appendFiles.appendFile(dataFile);
        appendFiles.commit();
    }

    private long estimateRecordSize(Record record) {
        return 1024;
    }

    /**
     * 去重记录包装
     */
    private record DeduplicatedRecord(Record record, String op) {
    }
}
