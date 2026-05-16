package com.cyan.dataman.infra.flink;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Flink SQL Runner
 * <p>
 * Application 模式入口类。接收 SQL 脚本文件路径作为参数，
 * 逐条执行 SQL 语句（支持 CREATE TABLE、INSERT INTO 等）。
 * <p>
 * 在 Flink Kubernetes Operator 的 FlinkDeployment 中配置：
 * <pre>
 *   job:
 *     jarURI: local:///opt/flink/usrlib/sql-runner.jar
 *     entryClass: com.cyan.dataman.infra.flink.SqlRunner
 *     args: ["/opt/flink/sql/job.sql"]
 * </pre>
 *
 * @author cy.Y
 * @since 1.0.0
 */
public class SqlRunner {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: SqlRunner <sql-file-path>");
        }

        String sqlFile = args[0];
        String sql = Files.readString(Paths.get(sqlFile));

        // Application 模式下 getExecutionEnvironment() 自动识别当前集群环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 按分号分割多条 SQL 语句，逐条执行
        String[] statements = sql.split(";");
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            tableEnv.executeSql(trimmed);
        }
    }
}
