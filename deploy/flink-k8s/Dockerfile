# ============================================================
# 自定义 Flink 镜像（Application 模式）
# ============================================================
FROM flink:2.0.1-scala_2.12

# Kafka SQL Connector（Flink SQL 核心需要，通过 SPI 发现）
ADD https://repo1.maven.org/maven2/org/apache/flink/flink-sql-connector-kafka/4.0.1-2.0/flink-sql-connector-kafka-4.0.1-2.0.jar /opt/flink/lib/

# SqlRunner fat JAR（包含 SqlRunner + iceberg-flink-runtime + hadoop + kafka-connector）
# 放在 lib/ 而非 usrlib/，让 Flink parent classloader 统一加载，避免 ClassLoader 隔离导致
# iceberg-flink-runtime 无法访问 hadoop 类的问题
COPY cyan-dataman-flink-runner/target/sql-runner.jar /opt/flink/lib/

RUN chown -R flink:flink /opt/flink/lib/

USER flink
