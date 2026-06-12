package com.cyan.dataman.infra.flink;

import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Flink SQL Runner
 * <p>
 * Application 模式入口类。接收 SQL 脚本文件路径作为参数，
 * 分离执行 CREATE TABLE 和 INSERT INTO：所有 CREATE 先注册，
 * 所有 INSERT INTO 通过 StatementSet 一起提交，共享 Kafka Source。
 * <p>
 * 在 Flink Kubernetes Operator 的 FlinkDeployment 中配置：
 * <pre>
 *   job:
 *     jarURI: local:///opt/flink/lib/sql-runner.jar
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

        // 先去掉注释，避免注释和 SQL 混在同一 segment 被误判
        sql = removeComments(sql);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 分离 CREATE 和 INSERT 语句
        List<String> createStmts = new ArrayList<>();
        List<String> insertStmts = new ArrayList<>();

        String[] statements = sql.split(";");
        for (String statement : statements) {
            String trimmed = statement.trim().replaceAll("\\s+", " ");
            if (trimmed.isEmpty()) {
                continue;
            }
            String upper = trimmed.toUpperCase(Locale.ROOT);
            if (upper.startsWith("INSERT")) {
                insertStmts.add(trimmed);
            } else if (isExecutableMetadataStatement(upper)) {
                createStmts.add(trimmed);
            } else if (upper.startsWith("SELECT") || upper.startsWith("WITH")) {
                throw new IllegalArgumentException("Flink SQL Application 模式不支持直接执行 SELECT 查询，请改为 INSERT INTO sink_table SELECT ...");
            } else {
                throw new IllegalArgumentException("Flink SQL Application 模式不支持该语句: " + abbreviate(trimmed));
            }
        }

        if (insertStmts.isEmpty()) {
            throw new IllegalArgumentException("Flink SQL Application 模式至少需要一条 INSERT INTO 语句，请配置 Sink 表并使用 INSERT INTO ... SELECT ...");
        }

        // 先执行所有 CREATE TABLE（注册表元数据）
        for (String stmt : createStmts) {
            tableEnv.executeSql(stmt);
        }

        // 所有 INSERT INTO 通过 StatementSet 一起提交，共享 Source
        if (!insertStmts.isEmpty()) {
            StatementSet stmtSet = tableEnv.createStatementSet();
            for (String stmt : insertStmts) {
                stmtSet.addInsertSql(stmt);
            }
            stmtSet.execute();
        }
    }

    private static String removeComments(String sql) {
        String withoutBlockComments = sql.replaceAll("(?s)/\\*.*?\\*/", "");
        return withoutBlockComments.replaceAll("(?m)^\\s*--.*\\n?", "");
    }

    private static boolean isExecutableMetadataStatement(String upper) {
        return upper.startsWith("CREATE ")
                || upper.startsWith("DROP ")
                || upper.startsWith("ALTER ")
                || upper.startsWith("USE ")
                || upper.startsWith("SET ")
                || upper.startsWith("RESET ")
                || upper.startsWith("LOAD MODULE ")
                || upper.startsWith("UNLOAD MODULE ")
                || upper.startsWith("ADD JAR ")
                || upper.startsWith("REMOVE JAR ");
    }

    private static String abbreviate(String statement) {
        if (statement.length() <= 120) {
            return statement;
        }
        return statement.substring(0, 120) + "...";
    }
}
