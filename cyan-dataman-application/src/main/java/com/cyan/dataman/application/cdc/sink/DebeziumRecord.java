package com.cyan.dataman.application.cdc.sink;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Debezium CDC 消息解析结果
 * <p>
 * 从 Debezium JSON 消息中提取的结构化数据对象。
 * Debezium 消息格式：{"schema":..., "payload":{"source":{"db":"xxx","table":"xxx"},...}}
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
     * key: 字段名, value: 字段值
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
