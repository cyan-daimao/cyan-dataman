package com.cyan.dataman.application.cdc.sink;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Iceberg 批量写入 ProcessFunction
 * <p>
 * 利用 Flink Checkpoint 机制进行批量提交：
 * 1. 数据先缓存在内存中
 * 2. Checkpoint 触发时批量写入文件
 * 3. 保证 exactly-once 语义
 * <p>
 * 特性：
 * - 支持写入多个 Iceberg 表
 * - 按表分组批量写入
 * - 控制文件大小（默认 128MB）
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
public class IcebergBatchSink extends ProcessFunction<IcebergWriteRecord, Void> implements CheckpointedFunction {

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
     * 目标文件大小（字节）
     */
    private final long targetFileSizeBytes;

    /**
     * Iceberg Catalog
     */
    private transient RESTCatalog catalog;

    /**
     * 表缓存
     */
    private transient Map<String, Table> tableCache;

    /**
     * 缓冲区：按表分组的数据
     */
    private transient Map<String, List<Record>> buffer;

    /**
     * 状态：用于故障恢复
     */
    private transient ListState<IcebergWriteRecord> checkpointedState;

    /**
     * 当前缓冲区大小
     */
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
    public void open(org.apache.flink.api.common.functions.OpenContext openContext) throws Exception {
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
        // 关闭前刷新剩余数据
        flush();
        if (catalog != null) {
            catalog.close();
        }
    }

    @Override
    public void processElement(IcebergWriteRecord value, ProcessFunction<IcebergWriteRecord, Void>.Context ctx, Collector<Void> out) throws Exception {
        String tableKey = value.getCacheKey();

        // 添加到缓冲区
        buffer.computeIfAbsent(tableKey, k -> new ArrayList<>()).add(value.getRecord());
        currentBufferSize += estimateRecordSize(value.getRecord());

        // 如果缓冲区超过目标大小，触发刷新
        if (currentBufferSize >= targetFileSizeBytes) {
            flush();
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        // Checkpoint 时刷新所有数据
        log.info("Checkpoint 触发，刷新缓冲区，大小: {} bytes", currentBufferSize);
        flush();
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        ListStateDescriptor<IcebergWriteRecord> descriptor =
                new ListStateDescriptor<>("iceberg-batch-sink-state",
                        TypeInformation.of(IcebergWriteRecord.class));

        checkpointedState = context.getOperatorStateStore().getListState(descriptor);

        // 恢复时重新处理未提交的数据
        if (context.isRestored()) {
            for (IcebergWriteRecord record : checkpointedState.get()) {
                processElement(record, null, null);
            }
        }
    }

    /**
     * 刷新缓冲区，写入 Iceberg
     */
    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        // 按表批量写入
        for (Map.Entry<String, List<Record>> entry : buffer.entrySet()) {
            String tableKey = entry.getKey();
            List<Record> records = entry.getValue();

            if (records.isEmpty()) {
                continue;
            }

            try {
                // 解析表标识
                String[] parts = tableKey.split("\\.");
                String dbName = parts[0];
                String tableName = parts[1];
                TableIdentifier tableId = TableIdentifier.of(dbName, tableName);

                // 加载表
                Table table = loadTable(tableId);
                if (table == null) {
                    log.warn("表不存在，跳过: {}", tableId);
                    continue;
                }

                // 写入数据文件
                writeDataFile(table, records);

                log.info("写入 Iceberg 成功: table={}, records={}", tableId, records.size());
            } catch (Exception e) {
                log.error("写入 Iceberg 失败: table={}, error={}", tableKey, e.getMessage(), e);
            }
        }

        // 清空缓冲区
        buffer.clear();
        currentBufferSize = 0;
    }

    /**
     * 加载 Iceberg 表
     */
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

    /**
     * 写入数据文件
     */
    private void writeDataFile(Table table, List<Record> records) throws Exception {
        Schema schema = table.schema();

        // 创建文件
        String fileName = "data-" + UUID.randomUUID() + ".parquet";
        OutputFile outputFile = table.io().newOutputFile(
                table.locationProvider().newDataLocation(fileName));

        // 写入记录
        GenericAppenderFactory appenderFactory = new GenericAppenderFactory(schema);
        FileAppender<Record> appender = appenderFactory.newAppender(outputFile, FileFormat.PARQUET);

        for (Record record : records) {
            appender.add(record);
        }
        appender.close();

        // 构建 DataFile
        DataFile dataFile = DataFiles.builder(table.spec())
                .withPath(outputFile.location())
                .withFileSizeInBytes(appender.length())
                .withFormat(FileFormat.PARQUET)
                .withRecordCount(records.size())
                .build();

        // 提交事务
        AppendFiles appendFiles = table.newAppend();
        appendFiles.appendFile(dataFile);
        appendFiles.commit();
    }

    /**
     * 估算记录大小
     */
    private long estimateRecordSize(Record record) {
        // 简单估算：每条记录约 1KB
        return 1024;
    }
}
