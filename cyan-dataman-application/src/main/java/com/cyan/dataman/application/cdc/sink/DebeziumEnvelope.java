package com.cyan.dataman.application.cdc.sink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Debezium 消息信封（外层包装）
 * <p>
 * Debezium 消息格式: {"schema": {...}, "payload": {...}}
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebeziumEnvelope {

    /**
     * 消息 Schema 定义（描述 payload 的字段结构）
     */
    private Map<String, Object> schema;

    /**
     * 消息体（包含实际的变更数据）
     */
    private DebeziumPayload payload;
}
