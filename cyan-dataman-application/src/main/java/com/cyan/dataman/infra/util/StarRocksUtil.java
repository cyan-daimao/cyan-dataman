package com.cyan.dataman.infra.util;

import com.cyan.arch.common.util.Convert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SR工具类
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Component
public class StarRocksUtil {

    /**
     * 数据库连接URL
     */
    @Value("${starrocks.url}")
    private String url;

    /**
     * 数据库用户名
     */
    @Value("${starrocks.username}")
    private String username;

    /**
     * 数据库密码
     */
    @Value("${starrocks.password}")
    private String password;

    /**
     * 执行查询 SQL，返回 List<Map<String, Object>>
     *
     * @param sql 任意 SELECT 语句
     * @return 查询结果，每行是一个 Map<列名, 值>
     * @throws SQLException 数据库异常
     */
    public List<Map<String, Object>> queryForList(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> result = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    if (value instanceof LocalDateTime) {
                        value = Convert.toDateTimeStr(value);
                    }
                    row.put(columnName, value);
                }
                result.add(row);
            }
            return result;
        }
    }

    /**
     * 执行带参数的查询，返回 List<Map<String, Object>>
     *
     * @param sql    带 ? 占位符的 SQL
     * @param params 参数数组
     * @return 结果列表
     * @throws SQLException 数据库异常
     */
    public List<Map<String, Object>> queryForList(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    result.add(row);
                }
                return result;
            }
        }
    }

    /**
     * 获取数据库连接
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * 安全关闭 ResultSet
     */
    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Failed to close ResultSet: {}", e.getMessage());
            }
        }
    }

    /**
     * 安全关闭 Statement
     */
    public static void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("Failed to close Statement: {}", e.getMessage());
            }
        }
    }

    /**
     * 安全关闭 Connection
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Failed to close Connection: {}", e.getMessage());
            }
        }
    }

    /**
     * 一次性关闭 Connection、Statement、ResultSet（按顺序关闭）
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        close(rs);
        close(stmt);
        close(conn);
    }
}
