package com.cyan.dataman.infra.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC 工具类，用于获取数据库连接和关闭资源。
 * 注意：实际项目中建议使用连接池（如 HikariCP、Druid）替代 DriverManager。
 */
public class JDBCUtil {

    // 数据库连接参数（建议从配置文件读取）
    private static final String URL = "jdbc:mysql://10.0.0.2:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "627459859@qq.com";

    static {
        try {
            // 加载驱动（JDBC 4.0+ 可省略，但显式加载更稳妥）
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC Driver not found!");
        }
    }

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /**
     * 安全关闭 ResultSet
     */
    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace(); // 或使用日志框架
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
                e.printStackTrace();
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
                e.printStackTrace();
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