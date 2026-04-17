package com.cyan.dataman.application.cdc.sink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Debezium 消息中的 source 信息
 * <p>
 * 描述变更事件的来源，包括数据库连接、binlog 位置等。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebeziumSource {

    /**
     * Debezium 连接器版本
     */
    private String version;

    /**
     * 连接器类型（如 mysql）
     */
    private String connector;

    /**
     * 连接器名称（即 Debezium connector 的逻辑名称）
     */
    private String name;

    /**
     * 变更事件时间戳（毫秒）
     */
    @JsonProperty("ts_ms")
    private String tsMs;

    /**
     * 快照标识：true=快照阶段，false=增量阶段，last=最后一次快照，schema_only=仅 Schema 快照
     */
    private String snapshot;

    /**
     * 源数据库名
     */
    private String db;

    /**
     * binlog 序列号
     */
    private String sequence;

    /**
     * 源表名
     */
    private String table;

    /**
     * MySQL 服务器 ID
     */
    @JsonProperty("server_id")
    private Long serverId;

    /**
     * MySQL GTID（全局事务标识）
     */
    private String gtid;

    /**
     * binlog 文件名
     */
    private String file;

    /**
     * binlog 位点
     */
    private Long pos;

    /**
     * binlog 行偏移量
     */
    private Long row;

    /**
     * 执行该 SQL 的线程 ID
     */
    private Long thread;

    /**
     * 执行的 SQL 语句（需开启 include.query 选项）
     */
    private String query;
}
