# Spark Connect CDC 实现方案

## 1. 背景与目标

CDC 模块同时预留了 Flink 和 Spark 两条路径（`SyncTool` 枚举）。Flink 路径已完整实现（Debezium → Kafka → Flink → Iceberg），Spark 路径仅有骨架（`SparkJobExecutor` 为 stub 模拟，`toggle()` 未区分 syncTool，调度未接入）。

目标：使用 Spark Connect（Spark 4.0）补全 Spark CDC 路径，实现批量 ETL / 历史回填 / 跨源关联。

## 2. 架构方案

```
Spring Boot (瘦客户端)                         Spark 集群 (计算引擎)
┌───────────────────┐                        ┌──────────────────────┐
│ SparkConfig        │  Spark Connect (gRPC)  │ Spark Connect Server │
│ .createSparkSession│ ────────────────────→  │ (常驻运行)            │
│                    │  sc://host:15002       │ ├─ 执行 Spark SQL     │
│ SparkJobExecutor   │                        │ ├─ 管理 Catalog       │
│ .executeSparkJob() │                        │ └─ 写入 Iceberg       │
│                    │                        └──────────────────────┘
│ SparkJobScheduler  │
│ @Scheduled 60s轮询  │
└───────────────────┘
```

核心思路：**Spring Boot 只管调度和触发，不在本地跑计算**。和 Flink remote 模式完全对称。

## 3. 关键设计决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| Catalog 注册 | 动态注册（CREATE OR REPLACE CATALOG） | 执行 SQL 前判断不存在才创建，服务端无需预配置 |
| SQL 生成 | 后端根据 syncMode 自动生成 | 用户体验好，不需要懂 Spark SQL |
| 调度方式 | Spring @Scheduled 轮询（60s） | 简单可靠，与现有 Flink 状态刷新一致 |
| SparkSession 管理 | 工厂方法，每次执行创建新实例 | 避免 Connect 连接泄漏 |

## 4. 数据流

```
用户 toggle 开启 / cron 触发
    │
    ▼
CdcConfigServiceImpl.toggle() 或 SparkJobScheduler.scheduleSparkJobs()
    │
    ▼
SparkJobExecutor.executeSparkJob(sparkJob, cdcConfig)
    ├── 1. 查 DsConfig 获取 MySQL JDBC 连接信息
    ├── 2. SparkConfig.createSparkSession() → Spark Connect 客户端
    ├── 3. spark.sql("CREATE OR REPLACE CATALOG mysql_{dsName} USING jdbc OPTIONS(...)")
    ├── 4. 根据 syncMode 生成 SQL：
    │     OVERWRITE → INSERT OVERWRITE TABLE rest.{schema}.{table} SELECT * FROM mysql_{ds}.{db}.{tbl}
    │     APPEND    → INSERT INTO TABLE rest.{schema}.{table} SELECT * FROM mysql_{ds}.{db}.{tbl}
    ├── 5. spark.sql(sql) → 发送到 Spark Connect Server 执行
    └── 6. 更新 CdcSparkTask 状态（SUCCESS/FAILED）
```

## 5. 改动清单

### 5.1 配置层

**文件**: `bootstrap-dev.yml` / `bootstrap-prod.yml` / `bootstrap-pre.yml`

新增 `spark.connect.url` 配置：

```yaml
spark:
  connect:
    url: sc://10.0.0.2:15002   # Spark Connect Server gRPC 地址
```

### 5.2 SparkConfig — 工厂方法 + Spark Connect

**文件**: `infra/config/SparkConfig.java`

- 移除 `@Bean getSparkSession()` 单例
- 改为工厂方法 `createSparkSession()`：通过 `.remote(sparkConnectUrl)` 创建 Spark Connect 客户端
- 保留 `createLocalSparkSession()`：本地模式，用于 Iceberg 表维护
- 保留 `@Bean localSparkSession()`：向后兼容（MetadataTableServiceImpl、IcebergMaintenanceScheduler 等注入）

### 5.3 SparkJobExecutor — 真实执行改造

**文件**: `application/cdc/job/SparkJobExecutor.java`

- 移除 `Thread.sleep(5000)` 模拟逻辑
- 移除 `SparkJobEvent` 事件驱动，改为直接同步执行
- 注入 `SparkConfig`、`DsConfigRepository`、`CdcConfigRepository`
- 核心流程：
  1. 创建 CdcSparkTask 实例（PENDING）
  2. 查 DsConfig 获取 MySQL 连接信息
  3. `SparkConfig.createSparkSession()` 获取 Spark Connect 客户端
  4. 动态注册 MySQL JDBC Catalog（`CREATE OR REPLACE CATALOG`）
  5. 根据 syncMode 生成 INSERT OVERWRITE / INSERT INTO SQL
  6. `spark.sql(sql)` 执行
  7. 更新 CdcSparkTask 状态

### 5.4 全链路移除 sparkSql 字段

SQL 由后端自动生成，移除全链路的 `sparkSql` 字段：

| 层级 | 文件 | 操作 |
|------|------|------|
| Cmd | `CdcSparkJobCmd.java` | 移除 sparkSql |
| Domain | `CdcSparkJob.java` | 移除 sparkSql + validate() |
| BO | `CdcSparkJobBO.java` | 移除 sparkSql |
| DTO | `CdcSparkJobDTO.java` | 移除 sparkSql |
| DO | `CdcSparkJobDO.java` | 移除 sparkSql |
| Event | `SparkJobEvent.java` | 移除 sparkSql |
| ServiceImpl | `CdcConfigServiceImpl.java` | updateSparkJob 移除 setSparkSql |

数据库迁移：`ALTER TABLE cdc_spark_job DROP COLUMN spark_sql;`

### 5.5 CdcConfigServiceImpl — toggle 适配 syncTool

**文件**: `application/cdc/impl/CdcConfigServiceImpl.java`

```java
public void toggle(String id, Boolean enabled) {
    CdcConfig config = cdcConfigRepository.findById(id);
    config.toggle(cdcConfigRepository, enabled);

    if (Boolean.TRUE.equals(enabled)) {
        if (SyncTool.FLINK.equals(config.getSyncTool())) {
            startConnectorForTable(config);
            cdcFlinkSyncService.enableCdcSync(config.getId());
        } else if (SyncTool.SPARK.equals(config.getSyncTool())) {
            triggerSparkSyncIfNeeded(config);  // 查关联 Spark Job，enabled=true 的执行一次
        }
    } else {
        if (SyncTool.FLINK.equals(config.getSyncTool())) {
            stopConnectorForTable(config);
            cdcFlinkSyncService.disableCdcSync(config.getId());
        } else if (SyncTool.SPARK.equals(config.getSyncTool())) {
            stopRunningTasks(config.getId());  // 停止运行中的任务
        }
    }
}
```

### 5.6 SparkJobScheduler — 定时调度

**文件**: `application/cdc/job/SparkJobScheduler.java`（新建）

- `@Scheduled(fixedDelay = 60000)` 每 60s 轮询
- 查询 `cdc_spark_job` 表中 `enabled=true` 且有 `cron_expression` 的记录
- 使用 Spring `CronExpression.parse()` 匹配当前时间
- 匹配成功且 CDC 配置已启用 → 调用 `sparkJobExecutor.executeSparkJob()`

### 5.7 Controller — 新增手动触发接口

**文件**: `adapter/cdc/http/CdcConfigController.java`

```
POST /api/v1/cdc/spark-jobs/{jobId}/execute  →  手动触发一次执行
```

### 5.8 Repository — 新增查询方法

| 接口 | 方法 | 用途 |
|------|------|------|
| `CdcSparkJobRepository` | `findAllEnabled()` | 调度器查询所有启用的 Spark Job |
| `CdcSparkTaskRepository` | `findRunningByCdcConfigId()` | toggle 停用时查找运行中任务 |

## 6. Flink vs Spark 使用场景

| 场景 | 引擎 | 特点 |
|------|------|------|
| 实时增量同步（DML 捕获） | Flink | 秒~分钟级延迟，持续运行 |
| 历史数据回填 | Spark | OVERWRITE 模式全量覆盖 |
| 批量 ETL / 跨源关联 | Spark | 用户自定义复杂 SQL |
| 定时聚合任务 | Spark | Cron 调度，如订单明细 → 日汇总 |
| 数据校正 / 重算 | Spark | 手动触发或 Cron |

## 7. 验证场景

1. 启动 Spark Connect Server，Spring Boot 通过 `sc://host:15002` 连接成功
2. 创建 SPARK 类型 CDC 配置，创建关联 Spark Job（syncMode=OVERWRITE, cron=0 */5 * * * *）
3. toggle 开启 → 动态注册 MySQL Catalog → 执行 INSERT OVERWRITE → Iceberg 表有数据
4. toggle 关闭 → 运行中的任务停止
5. 等待 5 分钟 → cron 触发第二次执行 → CdcSparkTask 新增一条记录
6. 切换为 APPEND 模式 → 执行 INSERT INTO → 数据追加
7. 手动触发 `POST /api/v1/cdc/spark-jobs/{jobId}/execute` → 立即执行一次
8. 重启 Spring Boot → Spark Connect Server 不受影响 → cron 继续触发

## 8. 远端 Spark Connect Server 配置

Spark 集群侧需启动 Spark Connect Server 并配置 Iceberg 支持：

```bash
./sbin/start-connect-server.sh \
  --master spark://spark-cluster:7077 \
  --conf spark.sql.catalog.rest=org.apache.iceberg.spark.SparkCatalog \
  --conf spark.sql.catalog.rest.catalog-impl=org.apache.iceberg.rest.RESTCatalog \
  --conf spark.sql.catalog.rest.uri=http://iceberg-rest:8181 \
  --conf spark.sql.catalog.rest.io-impl=org.apache.iceberg.aws.s3.S3FileIO \
  --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
  --jars /path/to/mysql-connector-j.jar
```

注意：需要在 Spark Connect Server 的 classpath 中包含 MySQL JDBC Driver。
