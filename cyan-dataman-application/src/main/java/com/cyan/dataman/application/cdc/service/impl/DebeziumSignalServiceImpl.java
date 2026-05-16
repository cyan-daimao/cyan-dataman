package com.cyan.dataman.application.cdc.service.impl;

import com.cyan.dataman.application.cdc.service.DebeziumSignalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Debezium 信号服务实现
 * 通过 JDBC 向 MySQL 源数据库发送增量快照信号
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
@Slf4j
public class DebeziumSignalServiceImpl implements DebeziumSignalService {

    private static final String SIGNAL_DATABASE = "debezium_cdc";
    private static final String SIGNAL_TABLE_NAME = "signal";

    @Override
    public boolean sendIncrementalSnapshotSignal(String hostname, String port, String username, String password, String dataTable) {
        return sendIncrementalSnapshotSignal(hostname, port, username, password, new String[]{dataTable});
    }

    @Override
    public boolean sendIncrementalSnapshotSignal(String hostname, String port, String username, String password, String[] dataTables) {
        if (dataTables == null || dataTables.length == 0) {
            log.warn("数据表列表为空，跳过发送增量快照信号");
            return false;
        }

        if (!ensureSignalTableExists(hostname, port, username, password)) {
            log.error("无法确保信号表存在，跳过发送增量快照信号");
            return false;
        }

        String dataCollections = Arrays.stream(dataTables)
                .map(table -> "\"" + table + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        String dataJson = String.format("{\"data-collections\": %s, \"type\": \"incremental\"}", dataCollections);
        String signalId = "snapshot-" + UUID.randomUUID().toString().substring(0, 8);

        String fullSignalTable = SIGNAL_DATABASE + "." + SIGNAL_TABLE_NAME;
        String sql = "INSERT INTO " + fullSignalTable + " (id, type, data) VALUES (?, 'execute-snapshot', ?)";

        log.info("发送增量快照信号: id={}, tables={}", signalId, Arrays.toString(dataTables));

        try (Connection conn = getConnectionToSignalDatabase(hostname, port, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, signalId);
            ps.setString(2, dataJson);

            int rows = ps.executeUpdate();
            boolean success = rows > 0;

            if (success) {
                log.info("增量快照信号发送成功: id={}, tables={}", signalId, Arrays.toString(dataTables));
            } else {
                log.warn("增量快照信号发送失败: id={}", signalId);
            }

            return success;
        } catch (SQLException e) {
            log.error("发送增量快照信号失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean ensureSignalTableExists(String hostname, String port, String username, String password) {
        String createDatabaseSql = "CREATE DATABASE IF NOT EXISTS " + SIGNAL_DATABASE + " DEFAULT CHARSET=utf8mb4";
        String createTableSql = "CREATE TABLE IF NOT EXISTS " + SIGNAL_DATABASE + "." + SIGNAL_TABLE_NAME + " (" +
                "  id VARCHAR(64) NOT NULL PRIMARY KEY, " +
                "  type VARCHAR(32) NOT NULL, " +
                "  data VARCHAR(2048)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = getConnectionWithoutDatabase(hostname, port, username, password);
             PreparedStatement psDb = conn.prepareStatement(createDatabaseSql);
             PreparedStatement psTable = conn.prepareStatement(createTableSql)) {

            psDb.executeUpdate();
            log.info("信号库已确保存在: {}", SIGNAL_DATABASE);

            psTable.executeUpdate();
            log.info("信号表已确保存在: {}.{}", SIGNAL_DATABASE, SIGNAL_TABLE_NAME);

            return true;
        } catch (SQLException e) {
            log.error("创建信号库或信号表失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private Connection getConnectionToSignalDatabase(String hostname, String port, String username, String password) throws SQLException {
        String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC",
                hostname, port, SIGNAL_DATABASE);
        return DriverManager.getConnection(url, username, password);
    }

    private Connection getConnectionWithoutDatabase(String hostname, String port, String username, String password) throws SQLException {
        String url = String.format("jdbc:mysql://%s:%s/?useSSL=false&serverTimezone=UTC",
                hostname, port);
        return DriverManager.getConnection(url, username, password);
    }
}
