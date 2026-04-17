# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 运行
java -jar cyan-dataman-application/target/cyan-dataman.jar

# 构建单个模块
mvn clean package -pl cyan-dataman-application -am -DskipTests

# 部署到 Nexus（需配置 ~/.m2/settings.xml 认证）
mvn clean deploy -DskipTests
```

本项目当前无测试代码。

## Architecture

**多模块 Maven 项目**，采用 DDD（领域驱动设计）分层架构：

```
cyan-dataman/
├── cyan-dataman-application/  # 主应用模块（Spring Boot）
└── cyan-dataman-client/       # 客户端 SDK（枚举、Feign RPC 接口）
```

### 分层结构（application 模块内）

```
adapter/         # HTTP 控制器、DTO 转换（AdapterConvert）
application/     # 业务编排、事务、领域服务（Cmd -> Domain -> BO）
domain/          # 领域模型、仓储接口、核心业务规则
infra/           # 仓储实现、MyBatis-Plus Mapper、外部集成配置
```

### 调用链与转换链路

每个写操作经过三层 MapStruct 转换，确保层间解耦：

```
Controller
  -> Service(Cmd) -> AppConvert.toDomain(cmd)
    -> DomainEntity.save(Repository)
      -> RepositoryImpl -> InfraConvert.toDO(entity) -> Mapper.insert(DO)
```

三组 Convert 职责：
- **AdapterConvert**：DTO <-> BO（adapter 层）
- **AppConvert**：Cmd -> Domain Entity，Domain Entity -> BO（application 层）
- **InfraConvert**：Domain Entity <-> DO（infra 层）

所有 Convert 均为 MapStruct 接口，使用 `INSTANCE = Mappers.getMapper(...)` 单例模式。

### 充血模型

领域实体采用充血模型，包含业务行为方法：

```java
// 领域实体示例
public class DsConfig {
    private String id;
    private String name;
    // ... 属性
    
    private void validate() {
        Assert.notBlank(this.name, new SilentException("名称不能为空"));
    }
    
    public DsConfig save(DsConfigRepository repository) {
        validate();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return repository.save(this);
    }
    
    public DsConfig update(DsConfigRepository repository) {
        validate();
        this.updatedAt = LocalDateTime.now();
        return repository.update(this);
    }
    
    public void delete(DsConfigRepository repository) {
        repository.deleteById(this.id);
    }
}
```

Service 层调用方式：`entity.save(repository)` 而非 `repository.save(entity)`。

### 核心约束

- **领域隔离**：`ds` 域和 `metadata` 域相互独立，禁止直接交叉引用。如需转换在 adapter 层处理。
- **多态值对象**：`ds` 域的 `ColumnValObj` 使用 Jackson `@JsonTypeInfo`，以 `databaseType` 字段作为判别器，反序列化为 `MysqlColumnValObj` 或 `PgsqlColumnValObj`。
- **无基类 DO**：所有 DO 类独立定义审计字段（`created_at`、`updated_at`、`deleted_at`），时间戳由领域实体的 `save()` 方法手动设置，未使用 MyBatis-Plus MetaObjectHandler。
- **逻辑删除**：使用 `@TableLogic(value = "null", delval = "now()")`，deleted_at 为 null 表示未删除，删除时设置为当前时间。

## Tech Stack

- Java 21, Spring Boot 3, MyBatis-Plus 3.5.7
- Apache Iceberg 1.10.1, Spark 4.0.2, Gravitino 1.1.0, Flink, Debezium, Kafka
- Nacos（服务发现与配置中心，通过 `spring.config.import: nacos:` 加载远程配置）
- MapStruct（对象映射），Lombok

## Dependencies

### 公共库依赖

项目依赖公司内部 `arch` 公共库（`com.cyan:arch-common`），提供：

- `Response<T>` — 统一响应封装
- `Page<T>` — 分页封装
- `Assert` — 断言工具
- `SilentException` — 静默异常（不输出堆栈）
- `MapstructConvert` — MapStruct 通用转换器

### Maven 私服

部署配置在 `pom.xml` 中：
- Releases: `http://nexus.cyan.com/repository/maven-releases/`
- Snapshots: `http://nexus.cyan.com/repository/maven-snapshots/`

需在 `~/.m2/settings.xml` 配置认证信息。

## Domains

### ds（数据源）
管理数据库连接（MySQL/PostgreSQL/Iceberg）。核心实体：`DsConfig`、`TableSchemaValObj`。

### metadata（元数据）
元数据目录，支持主题层级（最多 3 级）。核心实体：`MetadataSubject`、`MetadataTable`。

### cdc（变更数据捕获）
CDC 同步：源数据库 -> Iceberg，支持 Spark SQL 和 Flink + Debezium 两种同步工具。

核心实体：
- `CdcConfig` — 定义同步配置（源表 ds_id/db/table + 目标 Iceberg 表）
- `CdcSparkJob` — Spark SQL 模板，同步模式（OVERWRITE/APPEND），可选 Cron 调度
- `CdcSparkTask` — 任务实例，状态追踪（PENDING/RUNNING/SUCCESS/FAILED/STOPPED）

异步机制：
- Spark 任务通过 `ApplicationEventPublisher` 发布 `SparkJobEvent`，由 `SparkJobExecutor` 以 `@Async` + `@EventListener` 异步执行
- `CdcFlinkSyncServiceImpl` 定时刷新 Flink 同步状态（`@Scheduled` 每 30s）
- `IcebergMaintenanceScheduler` 执行 Iceberg 表维护（`@Scheduled` 每日零点）

## API Conventions

- 基础路径：`/api/v1`
- 响应格式：`Response<T>`（来自 `com.cyan.arch.common.api.Response`，属于 arch 公共库）
- 分页：`Page<T>`（来自 arch 公共库）
- 业务异常：`SilentException`（不输出堆栈）

## Naming Conventions

| 层级 | 命名模式 |
|---|---|
| Controller | `XxxController` |
| Service | `XxxService` / `XxxServiceImpl` |
| Repository | `XxxRepository`（接口，domain 层）/ `XxxRepositoryImpl`（实现，infra 层）|
| DTO | `XxxDTO`（adapter），`XxxBO`（application），`XxxDO`（infra）|
| 请求对象 | `XxxCmd`（写操作命令），`XxxQuery`（读操作查询，定义在 domain 层），`XxxValObj`（值对象）|
| 转换器 | `XxxAdapterConvert`（adapter），`XxxAppConvert`（application），`XxxInfraConvert`（infra）|

### Query 对象

查询对象定义在 `domain/xxx/query/` 目录，用于条件查询：

```java
// domain/ds/query/DsConfigListQuery.java
public class DsConfigListQuery {
    private String name;           // 模糊查询
    private DatasourceType datasourceType;  // 精确匹配
}
```

Repository 接口定义查询方法：
```java
List<DsConfig> list(DsConfigListQuery query);
DsConfig find(DsConfigFindQuery query);
```

## Configuration

配置文件位于 `cyan-dataman-application/src/main/resources/`：
- `bootstrap.yml` — 基础配置（应用名、profile、监控端点）
- `bootstrap-{profile}.yml` — 环境配置（dev/prod/pre），含 Nacos 地址、MySQL、Gravitino、RustFS(S3)、Iceberg REST、Debezium、Kafka 等
- `db/migration/` — SQL 建表脚本（Flyway/手动执行）
