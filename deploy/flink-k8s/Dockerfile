# ============================================================
# 自定义 Flink 镜像（包含 Kafka + Iceberg SQL Connector）
# 构建命令：docker build -t cyan/flink-sql:2.0.1 .
# ============================================================
FROM flink:2.0.1-scala_2.12

# Flink SQL 必需：Table Planner（包含 SQL 解析和优化器）
# 注意：Flink 2.0 的 planner 可能与运行时捆绑，此处预留扩展点

# Kafka SQL Connector（fat JAR，包含 Kafka 客户端和序列化器）
# 下载地址需根据实际版本调整
ADD https://repo1.maven.org/maven2/org/apache/flink/flink-sql-connector-kafka/4.0.1-2.0/flink-sql-connector-kafka-4.0.1-2.0.jar /opt/flink/lib/

# Iceberg Flink Runtime（fat JAR，包含 Iceberg 核心和 Parquet 支持）
ADD https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-flink-runtime-2.0/1.10.1/iceberg-flink-runtime-2.0-1.10.1.jar /opt/flink/lib/

# Hadoop AWS + AWS SDK（S3A 文件系统支持，用于 Checkpoint。Hadoop Client 由 sql-runner.jar 提供）
ADD https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/3.4.1/hadoop-aws-3.4.1.jar /opt/flink/lib/
ADD https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/1.12.767/aws-java-sdk-bundle-1.12.767.jar /opt/flink/lib/

# SqlRunner JAR（Application 模式入口，从项目构建产物复制）
# 构建前需先执行: mvn clean package -pl cyan-dataman-flink-runner -am
COPY cyan-dataman-flink-runner/target/sql-runner.jar /opt/flink/usrlib/

# 设置 JAR 权限
RUN chown -R flink:flink /opt/flink/lib/ && chown -R flink:flink /opt/flink/usrlib/

USER flink
