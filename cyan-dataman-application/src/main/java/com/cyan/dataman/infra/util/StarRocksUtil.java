package com.cyan.dataman.infra.util;

import com.cyan.arch.common.util.Convert;
import lombok.extern.slf4j.Slf4j;

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
public class StarRocksUtil {

    // 数据库连接参数（建议从配置文件读取）
    private static final String URL = "jdbc:mysql://10.0.0.2:30040/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";

    static {
        try {
            // 加载驱动（JDBC 4.0+ 可省略，但显式加载更稳妥）
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC Driver not found!");
        }
    }
    /**
     * 执行查询 SQL，返回 List<Map<String, Object>>
     *
     * @param sql 任意 SELECT 语句（不支持参数化，若需防注入请使用带参数版本）
     * @return 查询结果，每行是一个 Map<列名, 值>
     * @throws SQLException 数据库异常
     */
    public static List<Map<String, Object>> queryForList(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> result = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>(); // 保持列顺序
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i); // 使用别名（AS）
                    Object value = rs.getObject(i);
                    if (value instanceof LocalDateTime){
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
    public static List<Map<String, Object>> queryForList(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 设置参数
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
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, "");
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
     * 安全关闭 Statement（也适用于 PreparedStatement）
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
