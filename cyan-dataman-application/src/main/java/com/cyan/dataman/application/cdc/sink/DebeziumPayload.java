package com.cyan.dataman.application.cdc.sink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Debezium 消息中的 payload 部分
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebeziumPayload {

    /**
     * 变更事件来源信息（数据库连接、表名等）
     */
    private DebeziumSource source;

    /**
     * 操作类型：c=插入，r=快照读取，u=更新，d=删除
     */
    private String op;

    /**
     * 事件时间戳（毫秒）
     */
    @JsonProperty("ts_ms")
    private Long tsMs;

    /**
     * 事件时间戳（微秒）
     */
    @JsonProperty("ts_us")
    private Long tsUs;

    /**
     * 事务标识
     */
    private String transaction;

    /**
     * 变更后数据（动态字段，每张表不同）
     */
    private Map<String, Object> after;

    /**
     * 变更前数据（仅 update 和 delete 操作有值）
     */
    private Map<String, Object> before;
}
