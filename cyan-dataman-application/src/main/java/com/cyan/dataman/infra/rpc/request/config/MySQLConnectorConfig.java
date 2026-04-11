package com.cyan.dataman.infra.rpc.request.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * MySQL Debezium 连接器配置
 *
 * @author cy.Y
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MySQLConnectorConfig extends ConnectorConfig {

    /**
     * 连接器类
     */
    @JsonProperty("connector.class")
    private final String connectorClass = "io.debezium.connector.mysql.MySqlConnector";

    /**
     * debezium 生成 kafka topic 的前缀（必填，避免 topic 名称冲突）
     * 替代了旧版的 database.server.name，功能一致
     */
    @JsonProperty("topic.prefix")
    private String topicPrefix;

    /**
     * 任务最大数
     */
    @JsonProperty("tasks.max")
    private String taskMax;
/**
     * 数据库地址
     */
    @JsonProperty("database.hostname")
    private String hostname;

    /**
     * 数据库端口
     */
    @JsonProperty("database.port")
    private String port;

    /**
     * 数据库用户名
     */
    @JsonProperty("database.user")
    private String user;

    /**
     * 数据库密码
     */
    @JsonProperty("database.password")
    private String password;

    /**
     * 这个是模拟从数据库的节点id
     * 必须唯一
     */
    @JsonProperty("database.server.id")
    private int serverId;

    /**
     * 要同步的数据库名称
     */
    @JsonProperty("database.include.list")
    private String databaseIncludeList;
/**
     * 要同步的表名称
     */
    @JsonProperty("table.include.list")
    private String tableIncludeList;

    /**
     * kafka地址
     */
    @JsonProperty("schema.history.internal.kafka.bootstrap.servers")
    private String kafkaBootstrapServers;

    /**
     * kafka topic
     * 数据库结构变更的topic
     */
    @JsonProperty("schema.history.internal.kafka.topic")
    private String kafkaTopic;

    /**
     * 是否同步数据库结构
     */
    @JsonProperty("include.schema.changes")
    private boolean includeSchemaChanges;

    /**
     * 时间自适应，将DATETIME输出Long类型的时间戳
     */
    @JsonProperty("time.precision.mode")
    private final String timePrecisionMode = "connect";

    /**
     * 时间自适应，将DATETIME输出Long类型的时间戳
     */
    @JsonProperty("decimal.handling.mode")
    private final String decimalHandlingMode = "string";

    /**
     * 快照模式
     * when_needed: 当需要时执行快照（支持动态添加表）
     */
    @JsonProperty("snapshot.mode")
    private String snapshotMode;

    /**
     * 增量快照配置：信号数据集合（格式：db.table）
     * 用于支持动态添加表时触发增量快照
     */
    @JsonProperty("signal.data.collection")
    private String signalDataCollection;

    /**
     * 增量快照配置：信号数据源
     */
    @JsonProperty("signal.enabled.channels")
    private String signalEnabledChannels = "source";

    /**
     * 增量快照是否启用
     */
    @JsonProperty("incremental.snapshot.enabled")
    private Boolean incrementalSnapshotEnabled;

    /**
     * 增量快照块大小
     */
    @JsonProperty("incremental.snapshot.chunk.size")
    private String incrementalSnapshotChunkSize;

    /**
     * 增量快照水线间隙策略
     */
    @JsonProperty("incremental.snapshot.allow_null_values")
    private Boolean incrementalSnapshotAllowNullValues;
}
