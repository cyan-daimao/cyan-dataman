<!-- AGENTS.md for cyan-dataman -->
# cyan-dataman

`cyan-dataman` 是一个基于 DDD（领域驱动设计）分层架构的元数据与数据管理平台，采用 Java 21 + Spring Boot 3 构建。项目核心能力包括数据源管理、元数据目录管理、CDC 数据入湖同步。

---

## 项目概览

- **组织**: `com.cyan`
- **版本**: `1.0-SNAPSHOT`
- **模块数**: 2（`cyan-dataman-application`、`cyan-dataman-client`）
- **Java 源文件数**: `application` 模块 178 个，`client` 模块 22 个
- **测试代码**: **本项目当前没有任何测试代码**，`src/test` 目录在所有模块中均为空
- **自然语言**: 项目内所有注释、文档、需求文件均使用中文

### 核心领域

| 领域 | 说明 |
|------|------|
| **ds（数据源）** | 管理 MySQL、PostgreSQL、Iceberg 数据源的连接配置，支持数据库/表结构查询、DDL 变更、在线 SQL 执行 |
| **metadata（元数据）** | 通过主题（Subject）对数据表进行三级层级分类，管理元数据表、字段、索引信息，集成 Apache Gravitino 作为元数据目录，支持表关系图谱 |
| **cdc（变更数据捕获）** | 将源数据库表变更同步至 Iceberg 数据湖，支持 Spark SQL 批量同步与 Flink + Debezium 实时 CDC，内置任务状态追踪与 Iceberg 表维护调度 |

---

## 技术栈

| 技术 | 版本/说明 |
|------|----------|
| Java | 21 |
| Spring Boot | 3.3.13 |
| MyBatis-Plus | 3.5.7 |
| MapStruct | 编译期自动生成转换代码 |
| Lombok | 1.18.42（配合 `@Accessors(chain = true)` 实现链式调用） |
| Apache Iceberg | 1.10.1 |
| Apache Spark | 4.0.2（Spark Connect，Scala 2.13） |
| Apache Flink | 2.0.1 |
| Apache Gravitino | 1.1.0 |
| Nacos | 服务发现与配置中心（`spring.config.import: nacos:`） |
| MySQL | 8.x（驱动 `mysql-connector-j` 8.3.0） |
| Maven | 3.x（构建工具） |

其他关键依赖：
- `com.opencsv:opencsv` 5.12.0（CSV 解析）
- `org.apache.poi:poi-ooxml` 5.2.5（Excel 解析）
- `spring-cloud-starter-openfeign`（已引入，项目中仅 `cyan-dataman-client` 定义了 `TableRelationClient` Feign 接口）
- `hadoop-aws` 3.4.1、`software.amazon.awssdk:bundle` 2.25.20（S3A 文件系统）

内部公共库（`com.cyan` 组织，版本 `1.0-SNAPSHOT`）：
- `arch-common` — 提供 `Response<T>`、`Page<T>`、`Assert`、`SilentException`、`MapstructConvert`、`UserContextHolder`
- `arch-base` — Spring Boot 基础 Starter
- `cyan-employee-login` — 员工登录与上下文拦截
- `cyan-datagateway-client` — 数据网关客户端

---

## 项目结构

本项目是多模块 Maven 项目：

```
cyan-dataman/
├── pom.xml                               # 父 POM，定义依赖管理与 Nexus 部署仓库
├── cyan-dataman-client/                  # 客户端 SDK 模块（枚举、Feign 接口、DTO）
│   └── src/main/java/com/cyan/dataman/
│       ├── client/table/TableRelationClient.java          # 表关系 Feign 客户端
│       ├── client/table/dto/                              # 客户端 DTO
│       └── enums/                                         # 全量枚举定义
│
└── cyan-dataman-application/             # 主应用模块（Spring Boot 可执行 JAR）
    └── src/main/java/com/cyan/dataman/
        ├── Application.java              # Spring Boot 启动类
        ├── adapter/                      # 适配层：HTTP 控制器、DTO、AdapterConvert
        │   ├── cdc/http/
        │   ├── ds/http/
        │   └── metadata/
        │       ├── http/                 # 对外 REST API
        │       └── rpc/                  # 内部 Agent/RPC 接口
        ├── application/                  # 应用层：Service、BO、Cmd、AppConvert、事件/调度
        │   ├── cdc/
        │   │   ├── event/SparkJobEvent.java
        │   │   ├── job/SparkJobExecutor.java
        │   │   ├── job/SparkJobScheduler.java
        │   │   └── service/
        │   ├── ds/
        │   └── metadata/
        │       └── scheduler/IcebergMaintenanceScheduler.java
        ├── domain/                       # 领域层：充血实体、值对象、仓储接口、Query
        │   ├── cdc/
        │   ├── ds/
        │   └── metadata/
        └── infra/                        # 基础设施层：仓储实现、DO、Mapper、配置、工具类
            ├── config/
            ├── persistence/
            ├── rpc/request/               # Debezium RPC 请求对象
            └── util/
```

### 分层职责与对象转换链路

| 层级 | 职责 | 返回前端/RPC 的对象 |
|------|------|-------------------|
| **adapter** | HTTP 请求入口，参数接收与 DTO 转换，调用 application 层 | DTO |
| **application** | 业务流程编排、事务控制、领域服务调用、异步事件发布 | BO |
| **domain** | 充血领域模型（属性 + 业务行为）、定义仓储接口与 Query 对象 | Domain Entity |
| **infra** | 仓储实现、MyBatis-Plus Mapper、DO、外部 RPC 调用、工具类 | DO |

**写操作转换链路（不可跳过、不可逆序）**：
```
Controller
  -> Service(Cmd) -> AppConvert.toDomain(cmd)
    -> DomainEntity.save(Repository)
      -> RepositoryImpl -> InfraConvert.toDO(entity) -> Mapper.insert(DO)
```

**读操作转换链路**：
```
Mapper.selectList(DO)
  -> RepositoryImpl -> InfraConvert.toDomain(do)
    -> Service -> AppConvert.toBO(domain)
      -> Controller -> AdapterConvert.toDTO(bo) -> Response.success(dto)
```

### 转换器命名规范

| 转换器 | 所在层级 | 职责 |
|--------|---------|------|
| `XxxAdapterConvert` | adapter | DTO <-> BO |
| `XxxAppConvert` | application | Cmd -> Domain Entity；Domain Entity -> BO |
| `XxxInfraConvert` | infra | Domain Entity <-> DO |

所有 Convert 均为 MapStruct 接口，使用单例模式：
```java
@Mapper(uses = MapstructConvert.class)
public interface XxxConvert {
    XxxConvert INSTANCE = Mappers.getMapper(XxxConvert.class);
    // ...
}
```

---

## 架构规范

### 充血模型（铁律）

领域实体必须是充血模型，包含业务行为方法，禁止贫血模型。Service 层必须调用 `entity.save(repository)`，**禁止**直接调用 `repository.save(entity)`。

```java
public class DsConfig {
    // ... 属性

    private void validate() {
        Assert.notBlank(this.name, new SilentException("数据源名称不能为空"));
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

### 领域隔离

`ds`、`metadata`、`cdc` 三个领域相互独立，**禁止直接交叉引用**其他领域的值对象或实体。如需跨领域数据转换，必须在 adapter 层或 application 层完成。

例如：`domain/ds/valobj/ColumnValObj.java` 与 `domain/metadata/valobj/ColumnValObj.java` 是两个完全独立的类，不可互相引用。

### 多态值对象

`ds` 领域的 `ColumnValObj` 使用 Jackson `@JsonTypeInfo` 以 `databaseType` 字段作为判别器，反序列化为 `MysqlColumnValObj` 或 `PgsqlColumnValObj`。

```java
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "databaseType",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MysqlColumnValObj.class, name = "MYSQL"),
    @JsonSubTypes.Type(value = PgsqlColumnValObj.class, name = "POSTGRESQL")
})
public abstract class ColumnValObj { ... }
```

### DO 与逻辑删除

- **无基类 DO**：每个 DO 类独立定义审计字段（`created_at`、`updated_at`、`deleted_at`），时间戳由领域实体的 `save()` / `update()` 方法手动设置，**未使用** MyBatis-Plus MetaObjectHandler。
- **逻辑删除**：使用 `@TableLogic(value = "null", delval = "now()")`，`deleted_at` 为 `null` 表示未删除，删除时设置为当前时间。
- **枚举映射**：DO 中可直接声明枚举属性映射数据库 `varchar`，无需字符串转换。

---

## 命名规范

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| Controller | `XxxController` | `DsConfigController` |
| Service | `XxxService` / `XxxServiceImpl` | `DsConfigService` / `DsConfigServiceImpl` |
| Repository | `XxxRepository`（domain 层接口）/ `XxxRepositoryImpl`（infra 层实现） | `DsConfigRepository` / `DsConfigRepositoryImpl` |
| DTO | `XxxDTO`（adapter 层） | `DsConfigDTO` |
| BO | `XxxBO`（application 层） | `DsConfigBO` |
| DO | `XxxDO`（infra 层） | `DsConfigDO` |
| Cmd | `XxxCmd`（写操作入参，application 层） | `DsConfigCmd` |
| Query | `XxxQuery`（查询入参，domain 层） | `DsConfigListQuery` |
| 值对象 | `XxxValObj`（domain 层） | `ColumnValObj` |
| 转换器 | `XxxAdapterConvert` / `XxxAppConvert` / `XxxInfraConvert` | `DsConfigAdapterConvert` |

### 类头注释格式

```java
/**
 * 描述
 *
 * @author cy.Y
 * @since 1.0.0
 */
```

---

## API 规范

- 对外 REST API 基础路径：`/api/v1`
- 内部 Agent/RPC 基础路径：`/rpc/v1`
- 统一响应格式：`Response<T>`（来自 `com.cyan.arch.common.api.Response`）
- 分页：`Page<T>`（来自 `com.cyan.arch.common.api.Page`）

RESTful 风格示例：
- `POST /api/v1/ds` — 创建数据源
- `GET /api/v1/ds` — 列表
- `GET /api/v1/ds/{dsName}` — 详情
- `PUT /api/v1/ds/{dsName}` — 更新
- `DELETE /api/v1/ds/{dsName}` — 删除

---

## 构建与运行

### 构建命令

```bash
# 全量构建（跳过测试）
mvn clean package -DskipTests

# 构建单个模块及其依赖
mvn clean package -pl cyan-dataman-application -am -DskipTests

# 部署到 Nexus 私服（需在 ~/.m2/settings.xml 中配置 maven-releases / maven-snapshots 认证）
mvn clean deploy -DskipTests
```

### 运行命令

```bash
java -jar cyan-dataman-application/target/cyan-dataman.jar
```

### 构建配置要点

- 根 POM 使用 `maven-compiler-plugin` 3.8.1，编译参数保留 `-parameters`。
- `maven-source-plugin` 3.3.0 自动附加源码包（`jar-no-fork`）。
- application 模块使用 `spring-boot-maven-plugin` 3.3.13 重新打包为可执行 JAR（`cyan-dataman.jar`）。
- Spring Boot 插件排除了 `org.apache.hive:hive-common`，并设置 `hive-exec` 为解压依赖（解决 Jar 内类加载问题）。

---

## 测试说明

**本项目当前没有任何测试代码。** `cyan-dataman-application/src/test` 与 `cyan-dataman-client/src/test` 目录均为空。

构建时**务必**携带 `-DskipTests`。新增功能时如需补充测试，建议使用 JUnit 5 + Mockito，遵循项目现有包结构放置于 `src/test/java` 下。

---

## 配置文件

配置文件位于 `cyan-dataman-application/src/main/resources/`：

| 文件 | 说明 |
|------|------|
| `bootstrap.yml` | 基础配置（应用名、profile、Actuator 端点暴露、文件上传限制 100MB） |
| `bootstrap-dev.yml` | 开发环境配置（直连 VM IP `10.0.0.2`，Flink `local` 模式） |
| `bootstrap-pre.yml` | 预发环境配置（K8s 内部 Service DNS，Flink `remote` 模式） |
| `bootstrap-prod.yml` | 生产环境配置（K8s 内部 Service DNS，Flink `remote` 模式） |
| `core-site.xml` | 覆盖 Hadoop 3.4.x S3A 超时配置，避免 `NumberFormatException` |

> 注：数据库迁移脚本位于 `db/migration/` 目录下（无 Flyway 集成，需手动执行）。当前已有 `V1.0.1__cdc_refactor_flink_sql.sql`（CDC 模块 Flink SQL 化重构）。

### 配置隔离说明

- 开发环境（`dev`）直接连接 VM IP（`10.0.0.2`）。
- 预发/生产环境（`pre`/`prod`）连接 K8s 集群内部 DNS（如 `gravitino.gravitino.svc.cluster.local`）。
- Flink 在 dev 为 `local` 模式（嵌入 Spring Boot JVM），在 pre/prod 为 `remote` 模式（提交到 K8s Flink Session Cluster）。
- Flink K8s 部署配置位于项目根目录 `deploy/flink-k8s/`，包含 Dockerfile、Session Cluster YAML 和部署文档。

---

## 核心领域详细说明

### ds（数据源）

管理数据库连接配置，支持 MySQL、PostgreSQL、Iceberg。提供数据库列表查询、建库、表结构查询与变更、SQL 执行等功能。

核心实体：`DsConfig`、`TableSchemaValObj`。
创建表时自动添加 `created_at`、`updated_at`、`deleted_at` 三个默认字段。

### metadata（元数据）

元数据目录管理，支持主题三级层级结构（`level` 字段 1-3）。集成 Gravitino 进行元数据目录操作。支持表关系图谱（JOIN 路径查询）。

核心实体：`MetadataSubject`、`MetadataTable`。

### cdc（变更数据捕获）

CDC 同步：源数据库 -> Iceberg ODS 层。

**数据流架构**：
```
MySQL Source
  → Debezium Connector（每数据源一个）
    → Kafka Topic: cdc-{dsName}.{db}.{table}
      → Flink SQL Job（每数据源一个，Application Mode on K8s）
        → ODS Iceberg Table: ods.cdc_raw_{dsName}
          → Schema: (_raw_json, _op, _ts, _db, _table, _ingestion_time)
```

核心实体：
- `CdcConfig` — 同步配置（源表 + 同步工具 SPARK/FLINK）
- `CdcSparkJob` — Spark SQL 模板、同步模式（OVERWRITE/APPEND）、Cron 表达式
- `CdcSparkTask` — Spark 任务实例，状态追踪（PENDING/RUNNING/SUCCESS/FAILED/STOPPED）
- `CdcFlinkJob` — Flink 作业配置（**一数据源一作业**，通过 `dsName` 关联）

**Flink CDC（ODS 层）**：
- **技术选型**：Flink SQL（`StreamTableEnvironment.executeSql()`）替代原 DataStream API
- **ODS 表 Schema（统一）**：
  - `_raw_json STRING` — 完整 Debezium JSON
  - `_op STRING` — 操作类型（c/u/d）
  - `_ts BIGINT` — 数据变更时间戳
  - `_db STRING` — 来源库名
  - `_table STRING` — 来源表名
  - `_ingestion_time TIMESTAMP_LTZ(3)` — 摄入时间
- **Kafka Source**：`format = 'raw'`，保留完整 JSON，通过 `JSON_VALUE` + `COALESCE` 提取元数据（兼容含/不含 schema 的 Debezium JSON）
- **Iceberg Sink**：`write.upsert.enabled = 'false'`，纯追加模式保留完整变更历史
- **动态加表**：无需重启 Flink Job，Kafka topic pattern (`cdc-{dsName}.*`) 自动匹配新表
- **部署模式**：
  - `dev`：local 模式，Flink 嵌入 Spring Boot JVM
  - `pre/prod`：remote 模式，提交到 K8s Flink Session Cluster（JobManager + TaskManager HPA）

**Spark CDC（批量同步）**：
- 通过 Spark Connect 连接远程 Spark 集群
- 动态注册 JDBC Catalog，执行 `INSERT [OVERWRITE|INTO]`
- 由 `SparkJobScheduler`（`@Scheduled` 每分钟）根据 Cron 表达式触发

**Iceberg 表维护**：
- `IcebergMaintenanceScheduler`（`@Scheduled` 每日零点）：过期快照清理、合并小文件、删除孤儿文件。

---

## 安全与注意事项

- **Nexus 认证**：部署到私服需要本地 Maven `settings.xml` 配置用户名密码，请勿将敏感凭据提交到代码仓库。
- **配置隔离**：生产环境配置（`bootstrap-prod.yml`）包含数据库、Nacos、S3、Kafka 等敏感连接信息，确保不会意外泄露。
- **SQL 执行接口**：`DsConfigController.executeSql` 支持在连接的数据源上执行任意 SQL，需确保上层有充分的权限控制。
- **监控端点**：`bootstrap.yml` 中 `management.endpoints.web.exposure.include: "*"` 暴露了所有 Actuator 端点，生产环境应收缩暴露范围。
- **主题层级限制**：元数据主题最多支持 3 级，超出应在 adapter 层拦截。
- **CDC 任务调度**：Spark/Flink 任务涉及实际计算资源调度，配置变更时需评估对集群的影响。
- **Flink SQL 兼容性**：`JSON_VALUE` 语法和 `raw` format 需在目标 Flink 版本上验证，不同版本行为可能有差异。
- **Hadoop S3A 兼容**：`core-site.xml` 覆盖了 Hadoop 3.4.x 中 duration 格式的 S3A 超时配置，若升级 Hadoop 版本需重新验证。

---

## 附加目录说明

- `vibecoding/` — 存放项目需求文档、Dify Agent 配置与提示词等非代码资产，与主代码构建无关。
- `deploy/flink-k8s/` — Flink Session Cluster K8s 部署配置（Dockerfile、YAML、部署文档）。
- `.claude/settings.local.json` — Claude IDE 本地设置文件。
