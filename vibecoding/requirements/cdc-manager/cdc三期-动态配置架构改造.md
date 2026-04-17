# CDC 三期：动态配置架构改造

## 1. 背景与目标

二期实现的 Flink CDC 存在以下核心问题：
- **DAG 静态**：topic 列表和表映射在启动时固定，新增表需要重启 Flink 作业
- **单例 Env**：StreamExecutionEnvironment 是单例 Bean，只能提交一个作业
- **单表无法独立控制**：disable 只是改数据库状态，实际 Flink 仍在消费该表数据
- **服务重启状态丢失**：内存 Map 清空，remote 模式下可能重复提交作业
- **写入不幂等**：使用 Append，重复消费产生重复数据

三期目标：实现动态增减同步表、每表独立控制、幂等写入、服务重启不影响。

## 2. 架构方案

```
一数据源一 Flink 作业
    ├── Kafka Source（topic pattern 匹配 cdc-{dsName}.*，自动涵盖新增 topic）
    ├── DebeziumToIcebergProcessFunction（每 30s 定时查库刷新路由配置）
    │     ├── enabledTables：当前启用的表集合（dbName.tableName）
    │     └── tableToIcebergMapping：路由映射（dbName.tableName -> icebergSchema.icebergTableName）
    └── IcebergBatchSink（缓冲阶段按主键去重 + append 写入）
```

核心思路：**Flink 作业只管消费，路由和启停由 ProcessFunction 定时从数据库获取配置决定**。

## 3. 改动清单

### 3.1 FlinkConfig — 工厂方法（不再单例）

**文件**: `infra/config/FlinkConfig.java`

- 移除 `@Bean streamExecutionEnvironment()`
- 改为普通方法 `createStreamExecutionEnvironment()`，每次调用创建新 env
- CdcFlinkSyncServiceImpl 注入 FlinkConfig Bean，每次提交作业时调用工厂方法
- 保留 `getFlinkMode()` 和 `getFlinkRestUrl()` 供外部使用

### 3.2 CdcFlinkSyncServiceImpl — 生命周期管理重构

**文件**: `application/cdc/service/impl/CdcFlinkSyncServiceImpl.java`

#### Kafka Source 改用 topic pattern
```java
// 旧：精确 topic 列表（新增表需要重启作业）
.setTopics(topics)

// 新：topic pattern（自动匹配新增 topic）
.setTopicPattern(Pattern.compile("cdc-" + dsName + ".*"))
```
- 移除 `waitForTopicsCreated()` 方法
- 移除 `metadataTableRepository` 依赖（路由逻辑移到 ProcessFunction 内部）

#### 服务启动恢复
- `startFlinkSyncJob()` 从 `cdc_flink_job` 表查询 `status=RUNNING` 的作业
- 通过 `cdcConfigId` 反查 `dsName`，恢复 `dsNameToFlinkJobId` 映射
- remote 模式：通过 Flink REST API 校验作业是否仍在运行，已停止的标记 STOPPED
- 只对**没有运行中作业**的数据源提交新作业

#### enableCdcSync 简化
- 检查该数据源是否已有运行中的作业
- 没有 → 提交新作业
- 有 → 不操作，ProcessFunction 30s 内自动感知新表

#### disableCdcSync 简化
- 更新数据库 enabled=false
- 检查该数据源下是否还有启用的表
- 全部停了 → 取消 Flink 作业
- 还有启用的 → 不操作，ProcessFunction 30s 内自动跳过该表

### 3.3 DebeziumToIcebergProcessFunction — 动态路由核心改造

**文件**: `application/cdc/sink/DebeziumToIcebergProcessFunction.java`

#### 构造参数变更
```java
// 旧：启动时固定映射
DebeziumToIcebergProcessFunction(dsName, tableToIcebergMapping, icebergUri, ...)

// 新：传入 JDBC 连接信息，运行时动态查询
DebeziumToIcebergProcessFunction(dsName, icebergUri, ..., jdbcUrl, jdbcUsername, jdbcPassword)
```

#### 定时刷新配置
- `open()` 中启动 `ScheduledExecutorService`（每 30s）
- 通过 JDBC 查询 `cdc_config` 表获取该数据源下所有 enabled 的配置：
  ```sql
  SELECT db_name, table_name, iceberg_table_name FROM cdc_config
  WHERE ds_name = ? AND enabled = 1 AND sync_tool = 'FLINK' AND deleted_at IS NULL
  ```
- 查询 `metadata_table` 获取 Iceberg schema 信息构建完整映射
- 使用 `volatile` 保证 `tableToIcebergMapping` 和 `enabledTables` 的线程可见性

#### 动态路由
- `processElement()` 使用最新刷新后的映射进行路由
- 被移除的表的消息直接跳过
- 新增的表在下一个刷新周期自动开始路由
- 表缓存自动清理不再需要的条目

### 3.4 IcebergBatchSink — 按主键去重

**文件**: `application/cdc/sink/IcebergBatchSink.java`

#### 缓冲区结构变更
```java
// 旧：简单列表，同主键会重复写入
Map<String, List<Record>> buffer

// 新：按主键去重，同主键只保留最新记录
Map<String, Map<Object, DeduplicatedRecord>> buffer
```

#### 主键提取
- 优先使用 Iceberg 表的 `identifierFieldIds()`（如果设置了 identifier fields）
- 没有时用 schema 的第一个字段（通常是 id）

#### 操作处理
- insert/snapshot/read(op=c/r) → 写入
- update(op=u) → 用 after 数据覆盖，同主键自动去重
- delete(op=d) → 跳过不写入

### 3.5 CdcConfigServiceImpl — toggle 适配

**文件**: `application/cdc/impl/CdcConfigServiceImpl.java`

- toggle 启用：更新 Debezium connector + 调用 `cdcFlinkSyncService.enableCdcSync()`
  - FlinkSyncService 内部判断是否需要提交新作业
  - 已有作业时只打日志，ProcessFunction 自动感知
- toggle 停用：更新 Debezium connector + 调用 `cdcFlinkSyncService.disableCdcSync()`
  - 所有表都停了才取消 Flink 作业

## 4. 关键设计决策

| 决策点 | 选择 | 原因 |
|---|---|---|
| 动态刷新方式 | 定时查数据库（30s） | 简单可靠，不需要额外 Kafka topic |
| 幂等写入 | 缓冲阶段按主键去重 + append | 实现简单，配合 Debezium 端不重复推全量已足够 |
| Env 管理 | 工厂方法，每次创建新实例 | 避免单例限制，支持多数据源并行提交 |
| topic 消费 | topic pattern | 自动匹配新增 topic，无需等 topic 创建 |
| delete 处理 | 暂跳过（TODO） | 需要配合 Iceberg DeleteFiles，后续优化 |

## 5. 配置变更

```yaml
flink:
  mode: local           # local 或 remote
  rest:
    url: 10.0.0.2:20031 # Flink JobManager 地址（host:port）
  checkpoint:
    interval: 60000
    timeout: 600000
    dir: s3://flink/checkpoints/cyan-dataman
```

## 6. 验证场景

1. 启动 Spring Boot，toggle 开启表 A → Flink 作业提交，数据写入 Iceberg
2. 不停作业，toggle 开启表 B → Debezium 推 B 数据，Flink 30s 内自动消费并写入
3. toggle 停止表 B → Flink 30s 内自动跳过 B 的消息
4. 重启 Spring Boot → 远端 Flink 作业不受影响，映射从数据库恢复
5. 重复消费场景 → 按主键去重，无重复数据
