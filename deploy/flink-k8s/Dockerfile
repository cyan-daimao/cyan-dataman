# ============================================================
# 自定义 Flink 镜像（Application 模式）
# ============================================================
FROM flink:2.0.1-scala_2.12

# Kafka SQL Connector（Flink SQL 核心需要，通过 SPI 发现）
COPY deploy/flink-k8s/flink-sql-connector-kafka-4.0.1-2.0.jar /opt/flink/lib/

# SqlRunner fat JAR（包含 SqlRunner + iceberg-flink-runtime + hadoop + kafka-connector）
# 放在 lib/ 而非 usrlib/，让 Flink parent classloader 统一加载，避免 ClassLoader 隔离导致
# iceberg-flink-runtime 无法访问 hadoop 类的问题
COPY cyan-dataman-flink-runner/target/sql-runner.jar /opt/flink/lib/

# Flink S3 插件（checkpoint/savepoint 存储到 S3 需要）
RUN mkdir -p /opt/flink/plugins/flink-s3-fs-hadoop && \
    cp /opt/flink/opt/flink-s3-fs-hadoop-2.0.1.jar /opt/flink/plugins/flink-s3-fs-hadoop/ && \
    chown -R flink:flink /opt/flink/plugins/

RUN chown -R flink:flink /opt/flink/lib/

USER flink
