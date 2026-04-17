package com.cyan.dataman.application.cdc.sink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Debezium CDC 消息解析结果
 * <p>
 * 从 Debezium Envelope JSON 消息中提取的结构化数据对象。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DebeziumRecord {

    /**
     * 数据库名
     */
    private String dbName;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 操作类型
     * <ul>
     *   <li>c - create/insert</li>
     *   <li>r - read (snapshot)</li>
     *   <li>u - update</li>
     *   <li>d - delete</li>
     * </ul>
     */
    private String op;

    /**
     * 变更后的数据（after）
     */
    private Map<String, Object> afterData;

    /**
     * 变更前的数据（before），仅 update 和 delete 操作有值
     */
    private Map<String, Object> beforeData;

    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 从 DebeziumPayload 构建 DebeziumRecord
     */
    public static DebeziumRecord from(DebeziumPayload payload) {
        if (payload == null || payload.getSource() == null) {
            return null;
        }

        DebeziumSource source = payload.getSource();
        DebeziumRecord record = new DebeziumRecord();
        record.dbName = source.getDb();
        record.tableName = source.getTable();
        record.op = payload.getOp();
        record.timestamp = payload.getTsMs() != null ? payload.getTsMs() : System.currentTimeMillis();
        record.afterData = payload.getAfter();
        record.beforeData = payload.getBefore();
        return record;
    }

    /**
     * 获取表键（格式：dbName.tableName）
     */
    public String getTableKey() {
        return dbName + "." + tableName;
    }

    /**
     * 是否是删除操作
     */
    public boolean isDelete() {
        return "d".equals(op);
    }

    /**
     * 是否是更新操作
     */
    public boolean isUpdate() {
        return "u".equals(op);
    }

    /**
     * 是否是插入操作
     */
    public boolean isInsert() {
        return "c".equals(op);
    }

    /**
     * 是否是快照读取
     */
    public boolean isSnapshot() {
        return "r".equals(op);
    }
}
