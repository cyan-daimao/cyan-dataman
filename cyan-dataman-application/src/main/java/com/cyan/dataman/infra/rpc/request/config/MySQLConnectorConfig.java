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

    @JsonProperty("connector.class")
    private final String connectorClass = "io.debezium.connector.mysql.MySqlConnector";

    @JsonProperty("topic.prefix")
    private String topicPrefix;

    @JsonProperty("tasks.max")
    private String taskMax;

    @JsonProperty("database.hostname")
    private String hostname;

    @JsonProperty("database.port")
    private String port;

    @JsonProperty("database.user")
    private String user;

    @JsonProperty("database.password")
    private String password;

    @JsonProperty("database.server.id")
    private int serverId;

    @JsonProperty("database.include.list")
    private String databaseIncludeList;

    @JsonProperty("table.include.list")
    private String tableIncludeList;

    @JsonProperty("database.history.kafka.bootstrap.servers")
    private String kafkaBootstrapServers;

    @JsonProperty("database.history.kafka.topic")
    private String kafkaTopic;

    @JsonProperty("include.schema.changes")
    private boolean includeSchemaChanges;

    @JsonProperty("time.precision.mode")
    private final String timePrecisionMode = "connect";

    @JsonProperty("decimal.handling.mode")
    private final String decimalHandlingMode = "string";

    @JsonProperty("snapshot.mode")
    private String snapshotMode;

    @JsonProperty("signal.data.collection")
    private String signalDataCollection;

    @JsonProperty("signal.enabled.channels")
    private String signalEnabledChannels = "source";

    @JsonProperty("incremental.snapshot.enabled")
    private Boolean incrementalSnapshotEnabled;

    @JsonProperty("incremental.snapshot.chunk.size")
    private String incrementalSnapshotChunkSize;

    @JsonProperty("incremental.snapshot.allow_null_values")
    private Boolean incrementalSnapshotAllowNullValues;
}
