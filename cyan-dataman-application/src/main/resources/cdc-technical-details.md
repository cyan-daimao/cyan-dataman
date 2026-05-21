# CDC 技术细节文档

## 整体架构：Debezium + Kafka + Flink SQL on K8s

```
MySQL (binlog)
    │
    ▼
┌─────────────────────┐
│  Debezium Connector │  ← 通过 Debezium Connect REST API 管理
│  (Kafka Connect)    │
└─────────┬───────────┘
          │ 原始 JSON (含 payload.before/after/source/op)
          ▼
┌─────────────────────┐
│  Kafka Topic        │  ← 命名: cdc-{dsName}.{dbName}.{tableName}
│  (raw JSON)         │     由 Java 后端通过 AdminClient 预创建
└─────────┬───────────┘
          │
          ▼
┌─────────────────────────────────────────┐
│  FlinkDeployment (K8s CRD)             │  ← 由 Java 后端通过 fabric8 提交
│  ┌─────────────────────────────────┐   │
│  │  sql-runner.jar (SqlRunner)     │   │  ← fat JAR，含 Kafka/Iceberg/Hadoop/AWS 依赖
│  │  读取 /opt/flink/sql/job.sql    │   │  ← 从 ConfigMap 挂载
│  │  执行 Flink SQL:                │   │
│  │    1. CREATE CATALOG (Iceberg)  │   │
│  │    2. CREATE TABLE kafka_source │   │
│  │    3. CREATE TABLE iceberg_sink │   │
│  │    4. INSERT INTO sink SELECT   │   │
│  │       JSON_VALUE(...) FROM src  │   │
│  └─────────────────────────────────┘   │
│  JM: 0.5 CPU / 1G  TM: 0.5 CPU / 1G   │
└─────────────────────────────────────────┘
          │
          ▼
┌─────────────────────┐
│  Iceberg ODS 表      │  ← 通过 Gravitino REST Catalog 管理
│  (S3/RustFS 存储)    │     含业务字段 + 5 个元数据字段
└─────────────────────┘
```

---

## 一、Debezium 层：Binlog 采集

**核心文件**: `CdcConfigServiceImpl.java`

### 1.1 Connector 管理

- 通过 Feign RPC（`DebeziumRPC`）调用 Debezium Connect REST API 管理连接器
- **同一数据源共用一个 Connector**，命名规则：`cdc-{dsName}`（如 `cdc-mysql-x99`）
- Connector 类型：`io.debezium.connector.mysql.MySqlConnector`

### 1.2 多表共享机制

当同一数据源下有多个表需要 CDC 时：
- 所有表共享同一个 Connector
- 通过 `table.include.list` 参数控制包含哪些表（如 `cyan_databi.bi_dashboard,cyan_databi.bi_chart`）
- 新增表时更新 `table.include.list` → 重启 connector → 发增量快照信号
- 删除/停用最后一张表时，直接删除 Connector

### 1.3 快照策略

```java
// 关键配置
snapshot.mode: when_needed            // 有需要就做全量快照
incremental.snapshot.enabled: true    // 启用增量快照
signal.data.collection: debezium_cdc.signal  // 信号表
incremental.snapshot.chunk.size: 1024 // 每批 1024 行
```

两种场景：
1. **Connector 不存在（首次/删除后重建）**：Debezium 自动对 `include.list` 中的表做全量快照
2. **Connector 已存在（新增表）**：通过 `DebeziumSignalService` 向 MySQL 源库的 `debezium_cdc.signal` 表插入信号记录，触发对该表的增量快照

### 1.4 Kafka Topic 管理

- 命名格式：`cdc-{dsName}.{dbName}.{tableName}`（如 `cdc-mysql-x99.cyan_databi.bi_dashboard`）
- Java 后端通过 `Kafka AdminClient` 预创建 Topic（避免 Kafka `auto.create.topics.enable=false` 导致 Debezium 写入失败）
- **CDC 关闭时删除 Topic**，确保重新开启时从零消费、无重复数据

---

## 二、Flink SQL 生成

**核心文件**: `CdcFlinkSyncServiceImpl.java`

### 2.1 生成的 SQL 结构

`buildFlinkSql()` 拼出三段 SQL：

**第一段 — Iceberg REST Catalog：**
```sql
CREATE CATALOG IF NOT EXISTS rest WITH (
  'type' = 'iceberg',
  'catalog-type' = 'rest',
  'uri' = 'http://iceberg-rest.cyan.com/iceberg',  -- Gravitino
  's3.endpoint' = 'http://10.0.0.2:9000',           -- RustFS (MinIO 兼容)
  's3.access-key-id' = '...',
  's3.secret-access-key' = '...',
  's3.path-style-access' = 'true'
);
```

**第二段 — Kafka Source（单字段 raw JSON）：**
```sql
CREATE TABLE IF NOT EXISTS kafka_source (
  _raw_json STRING
) WITH (
  'connector' = 'kafka',
  'topic' = 'cdc-mysql-x99.cyan_databi.bi_dashboard',
  'properties.bootstrap.servers' = 'kafka:9092',
  'properties.group.id' = 'flink-cdc-mysql-x99-cyan_databi-bi_dashboard',
  'scan.startup.mode' = 'earliest-offset',
  'format' = 'raw'        -- 原始字符串，不做反序列化
);
```

设计关键：**不用 Debezium 的 Avro/JSON schema 反序列化，而是 raw format 读取完整 JSON 字符串**，然后在 INSERT SELECT 中用 `JSON_VALUE()` 逐字段提取。好处：
- 不依赖 Schema Registry
- SQL 对字段变化更灵活
- 任何 Debezium JSON 格式变动不会导致 Source 挂掉

**第三段 — Iceberg Sink + INSERT SELECT（`buildSinkSql()`）：**

```sql
-- Sink 表定义
CREATE TABLE IF NOT EXISTS rest.ods.ods_cdc_raw_xxx (
  `id` BIGINT,
  `name` STRING,
  `created_at` TIMESTAMP_LTZ(3),
  -- ... 业务字段
  `_op` STRING,
  `_ts` BIGINT,
  `_db` STRING,
  `_table` STRING,
  `_ingestion_time` TIMESTAMP_LTZ(3)
);

-- 数据提取与写入
INSERT INTO rest.ods.ods_cdc_raw_xxx
SELECT
  CAST(JSON_VALUE(_raw_json, '$.payload.after.id') AS BIGINT) AS `id`,
  JSON_VALUE(_raw_json, '$.payload.after.name') AS `name`,
  TO_TIMESTAMP_LTZ(CAST(JSON_VALUE(_raw_json, '$.payload.after.created_at') AS BIGINT), 3) AS `created_at`,
  -- ... 业务字段提取
  COALESCE(JSON_VALUE(_raw_json, '$.payload.op'), JSON_VALUE(_raw_json, '$.op')) AS `_op`,
  CAST(COALESCE(JSON_VALUE(_raw_json, '$.payload.ts_ms'), JSON_VALUE(_raw_json, '$.ts_ms')) AS BIGINT) AS `_ts`,
  COALESCE(JSON_VALUE(_raw_json, '$.payload.source.db'), JSON_VALUE(_raw_json, '$.source.db')) AS `_db`,
  COALESCE(JSON_VALUE(_raw_json, '$.payload.source.table'), JSON_VALUE(_raw_json, '$.source.table')) AS `_table`,
  NOW() AS `_ingestion_time`
FROM kafka_source;
```

### 2.2 类型映射

`DebeziumTypeMapper` 负责将 MySQL 类型转为 Flink SQL 类型：

| MySQL 类型 | Flink SQL 类型 | 提取表达式 |
|---|---|---|
| INT/INTEGER | INT | `CAST(... AS INT)` |
| BIGINT | BIGINT | `CAST(... AS BIGINT)` |
| DECIMAL(p,s) | DECIMAL(p,s) | `CAST(... AS DECIMAL(p,s))` |
| DATETIME/TIMESTAMP | TIMESTAMP_LTZ(3) | `TO_TIMESTAMP_LTZ(CAST(... AS BIGINT), 3)` |
| VARCHAR/TEXT/CHAR | STRING | 直接 `JSON_VALUE()` |
| BLOB/BINARY | BYTES | `CAST(... AS BYTES)` |

### 2.3 ODS 表额外字段

每个 Sink 表除了业务字段外，追加 5 个元数据字段：

| 字段 | 类型 | 来源 | 用途 |
|---|---|---|---|
| `_op` | STRING | `$.payload.op` | 操作类型：c(创建)/r(快照读取)/u(更新)/d(删除) |
| `_ts` | BIGINT | `$.payload.ts_ms` | 变更时间戳（毫秒） |
| `_db` | STRING | `$.payload.source.db` | 源数据库名 |
| `_table` | STRING | `$.payload.source.table` | 源表名 |
| `_ingestion_time` | TIMESTAMP_LTZ(3) | `NOW()` | 入库时间 |

---

## 三、FlinkDeployment CRD 提交

### 3.1 作业提交流程

```
enableCdcSync(cdcConfigId)
    │
    ├── ensureOdsTableExists(config)          ← 通过元数据服务确保 ODS 表已创建
    │
    ├── buildFlinkSql(config)                 ← 拼装 Flink SQL
    │
    ├── createOrUpdateConfigMap(name, sql)    ← SQL 存入 K8s ConfigMap
    │       ConfigMap: {deploymentName}-sql
    │       Data: { "job.sql": "...完整SQL..." }
    │
    ├── buildFlinkDeploymentYaml(name, cm)    ← 拼 FlinkDeployment CRD YAML
    │
    └── k8sClient.load(yaml).createOrReplace()  ← 提交到 K8s，Operator 自动拉起 JM + TM
```

### 3.2 FlinkDeployment YAML 关键配置

```yaml
spec:
  image: harbor.cyan.com/cyan/flink-sql:2.0.1   # 自定义镜像，内含 sql-runner.jar
  flinkVersion: v2_0
  flinkConfiguration:
    state.backend.type: rocksdb                    # RocksDB 状态后端
    state.checkpoints.dir: s3://flink/checkpoints/...  # S3(RustFS) 存储 checkpoint
    execution.checkpointing.interval: 60s          # 每 60s 做 1 次 checkpoint
    execution.checkpointing.mode: EXACTLY_ONCE     # 精确一次语义
    s3.endpoint: http://10.0.0.2:9000              # RustFS 对象存储
  job:
    jarURI: local:///opt/flink/lib/sql-runner.jar  # 镜像内置的 JAR
    entryClass: com.cyan.dataman.infra.flink.SqlRunner
    args: ["/opt/flink/sql/job.sql"]               # ConfigMap 挂载的 SQL 文件
    parallelism: 1
    upgradeMode: last-state                        # 升级时从最近 checkpoint 恢复
  podTemplate:
    spec:
      volumes:
        - name: sql-volume
          configMap:
            name: cdc-xxx-sql                      # 挂载 ConfigMap → /opt/flink/sql/
```

### 3.3 命名规则

| 资源 | 命名 |
|---|---|
| FlinkDeployment | `cdc-{dsName}-{dbName}-{tableName}`（RFC 1123 格式） |
| ConfigMap | `{deploymentName}-sql` |
| Kafka Topic | `cdc-{dsName}.{dbName}.{tableName}` |
| Kafka Group ID | `flink-cdc-{dsName}-{dbName}-{tableName}` |
| Iceberg ODS 表 | `ods_cdc_raw_{subject}_{db}_{table}` |

### 3.4 一表一 Deployment 设计

每张需要 CDC 的表对应一个独立的 FlinkDeployment：
- 表之间完全隔离，单表故障不影响其他表
- 可以独立重启、扩缩容
- SQL 简单清晰（一个 Source + 一个 Sink）

---

## 四、SqlRunner：Flink Application 入口

**文件**: `flink-runner/SqlRunner.java`

这是整个模块唯一的 Java 类，逻辑非常精简：

```
main(args)
  │
  ├── 读取 SQL 文件 (args[0] = /opt/flink/sql/job.sql)
  ├── 去掉单行注释 (-- 开头)
  ├── 按 ; 分割所有语句
  ├── 分类：CREATE 语句 / INSERT 语句
  ├── 逐一执行所有 CREATE（注册 Catalog、Source 表、Sink 表）
  └── 所有 INSERT 放入 StatementSet 一次性提交
```

**StatementSet 的意义**：如果 SQL 中有多个 INSERT 语句，使用 StatementSet 可以让它们共享 Kafka Source 连接，避免重复消费同一 topic。

---

## 五、状态监控与生命周期

### 5.1 定时状态刷新

`refreshSyncStatus()` 每 30 秒执行一次，检查所有 RUNNING 状态的 FlinkJob：
- 对应的 FlinkDeployment 是否还在 K8s 中
- 如果已被删除，更新状态为 STOPPED

### 5.2 启停控制

| 操作 | 流程 |
|---|---|
| **启用** | ODS 建表 → 创建 ConfigMap → 提交 FlinkDeployment → 记录 FlinkJob |
| **停用** | 删除 FlinkDeployment → 删除 ConfigMap → 更新 FlinkJob 状态为 STOPPED |
| **重启** | 删除旧 Deployment → 等待 K8s 删除完成(最多30s) → 重新提交 |
| **删除配置** | 删 DB 记录 → 若无其他表则删 Connector，否则更新 include.list |

### 5.3 精确一次语义保障

- Flink checkpoint 存储在 S3(RustFS)
- `upgradeMode: last-state` 确保作业重启时从最近 checkpoint 恢复
- Kafka Source 的 `scan.startup.mode: earliest-offset` + checkpoint offset 确保不丢数据
- CDC 关闭时删 Kafka Topic + 清 Iceberg ODS 表，重开时全量重写，避免数据不一致

---

## 六、数据流示例

以 `bi_dashboard` 表启用 Flink CDC 为例：

```
1. MySQL bi_dashboard 表发生 UPDATE
       │
2. Debezium 捕获 binlog → 写入 Kafka Topic:
   cdc-mysql-x99.cyan_databi.bi_dashboard
       │
   消息内容:
   {
     "payload": {
       "before": { "id": 1, "name": "旧名称" },
       "after":  { "id": 1, "name": "新名称" },
       "op": "u",
       "ts_ms": 1700000000000,
       "source": { "db": "cyan_databi", "table": "bi_dashboard" }
     }
   }
       │
3. Flink SqlRunner (JM+TM Pod) 执行:
   SELECT JSON_VALUE(_raw_json, '$.payload.after.id') ...  FROM kafka_source
   → 提取字段 → 写入 Iceberg rest.ods.ods_cdc_raw_xxx
       │
4. Iceberg ODS 表中新增一行:
   id=1, name="新名称", _op="u", _ts=1700000000000,
   _db="cyan_databi", _table="bi_dashboard", _ingestion_time=NOW()
```

---

## 七、关键代码文件索引

| 层级 | 文件 | 职责 |
|---|---|---|
| **API 层** | `infra/rpc/request/DebeziumRPC.java` | Debezium Connect REST API Feign 客户端 |
| **API 层** | `infra/rpc/request/config/MySQLConnectorConfig.java` | MySQL Debezium 连接器完整配置 |
| **Service 层** | `application/cdc/impl/CdcConfigServiceImpl.java` | CDC 配置 CRUD、Debezium/Kafka/Flink 全生命周期管理 |
| **Service 层** | `application/cdc/service/impl/CdcFlinkSyncServiceImpl.java` | Flink SQL 生成、FlinkDeployment CRD 构建/提交/删除 |
| **Service 层** | `application/cdc/service/impl/DebeziumSignalServiceImpl.java` | 增量快照信号发送 |
| **Service 层** | `application/cdc/service/CdcSchemaSyncService.java` | Schema 变更检测与 Iceberg 表同步 |
| **工具类** | `infra/util/DebeziumTypeMapper.java` | MySQL → Flink SQL 类型映射 |
| **工具类** | `infra/util/DsJdbcUtil.java` | 数据源 JDBC 工具（获取表结构） |
| **Flink 模块** | `flink-runner/SqlRunner.java` | Flink Application 入口，解析并执行 SQL 文件 |
| **Flink 模块** | `flink-runner/pom.xml` | 依赖管理：Flink 2.0.1 + Kafka Connector + Iceberg + Hadoop + AWS SDK |
| **Domain 层** | `domain/cdc/CdcConfig.java` | CDC 配置实体 |
| **Domain 层** | `domain/cdc/CdcFlinkJob.java` | Flink 作业记录实体 |
