# cyan-dataman 项目指南

## 项目概述

cyan-dataman 是一个元数据管理平台，基于 DDD（领域驱动设计）架构，集成 Apache Iceberg 和 Apache Gravitino，提供元数据主题管理、表管理、快照回滚等功能。

## 技术栈

- **Java 21** - 基于最新 LTS 版本
- **Spring Boot 3** - 基础框架
- **MyBatis-Plus 3.5.7** - ORM 框架
- **Apache Iceberg** - 数据湖表格式
- **Apache Spark** - 数据处理引擎
- **Apache Gravitino** - 元数据目录管理
- **Nacos** - 服务注册与配置中心

## 项目结构

```
cyan-dataman/
├── cyan-dataman-application/    # 应用模块（主模块）
│   └── src/main/java/com/cyan/dataman/
│       ├── adapter/             # 适配层
│       │   ├── integration/     # 外部系统集成
│       │   └── metadata/        # 元数据相关适配器
│       │       ├── http/        # REST API 控制器
│       │       └── rpc/         # RPC 接口
│       ├── application/         # 应用层
│       │   └── metadata/        # 元数据服务
│       │       ├── impl/        # 服务实现
│       │       ├── bo/          # 业务对象
│       │       └── cmd/         # 命令对象
│       ├── domain/              # 领域层
│       │   └── metadata/        # 元数据领域
│       │       ├── query/       # 查询对象
│       │       ├── repository/  # 仓储接口
│       │       └── valobj/      # 值对象
│       └── infra/               # 基础设施层
│           ├── config/          # 配置类
│           ├── persistence/     # 持久化实现
│           └── util/            # 工具类
└── cyan-dataman-client/         # 客户端 SDK
    └── src/main/java/com/cyan/dataman/
        ├── client/              # 客户端实现
        ├── enums/               # 枚举定义
        └── request/             # 请求对象
```

## 核心领域模型

### MetadataSubject（元数据主题）

元数据主题用于对数据表进行分类管理，支持三级层级结构。

**关键字段：**
- `subjectCode` - 主题编码（唯一）
- `subjectName` - 主题名称（唯一）
- `parentId` - 父主题 ID
- `level` - 层级（1-3级）
- `owner` - 负责人
- `openStatus` - 开启状态

### MetadataTable（元数据表）

表示一个数据表的元数据信息。

**关键字段：**
- `name` - 表名
- `subjectCode` - 所属主题编码
- `layerCode` - 数据层编码
- `datasourceType` - 数据源类型
- `owner` - 负责人
- `heatLevel` - 热度等级
- `secretLevel` - 秘密等级
- `onlineStatus` - 在线状态

## API 设计规范

### RESTful API

所有 API 遵循 RESTful 规范，基础路径为 `/api/v1`。

**示例：**
```
GET    /api/v1/metadata/tables          # 分页查询表列表
GET    /api/v1/metadata/tables/{id}     # 查询单个表
POST   /api/v1/metadata/tables          # 创建表
PUT    /api/v1/metadata/tables/{id}     # 更新表
DELETE /api/v1/metadata/tables/{id}     # 删除表
GET    /api/v1/metadata/tables/tree     # 获取主题-表树形结构
```

### 响应格式

统一使用 `Response<T>` 包装响应：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## 开发规范

### 分层架构原则

1. **adapter 层**：处理 HTTP/RPC 请求，负责 DTO 转换
2. **application 层**：业务逻辑编排，事务管理
3. **domain 层**：领域模型，核心业务规则
4. **infra 层**：技术实现，数据库访问

### 命名规范

- **Controller**：`XxxController`
- **Service**：`XxxService` / `XxxServiceImpl`
- **Repository**：`XxxRepository` / `XxxRepositoryImpl`
- **DTO**：`XxxDTO`（适配层）、`XxxBO`（应用层）、`XxxDO`（基础设施层）
- **Query**：`XxxQuery`
- **Cmd**：`XxxCmd`
- **ValObj**：`XxxValObj`

### 代码风格

- 使用 Lombok 减少样板代码
- 使用 MapStruct 进行对象转换
- 领域模型包含业务方法（如 `save()`, `update()`, `delete()`）
- 使用链式调用（`@Accessors(chain = true)`）

## 数据库

### 表命名

- `metadata_subject` - 元数据主题表
- `metadata_table` - 元数据表
- `metadata_column` - 元数据字段
- `metadata_index` - 元数据索引

### 逻辑删除

所有表使用 `deleted_at` 字段实现逻辑删除。

## 枚举类型

主要枚举定义在 `cyan-dataman-client` 模块：

| 枚举 | 说明 |
|------|------|
| `DatasourceType` | 数据源类型 |
| `DataLayer` | 数据层（ODS/DWD/DWS/ADS） |
| `HeatLevel` | 热度等级 |
| `SecretLevel` | 秘密等级 |
| `OnlineStatus` | 在线状态 |
| `OpenStatus` | 开启状态 |
| `PartitionType` | 分区类型 |
| `StorageType` | 存储类型 |
| `WriteMode` | 写入模式 |

## 配置文件

- `bootstrap.yml` - 主配置
- `bootstrap-dev.yml` - 开发环境配置
- `bootstrap-prod.yml` - 生产环境配置

## 构建与部署

### 构建

```bash
mvn clean package -DskipTests
```

### 部署

项目使用 Nexus 作为 Maven 仓库，支持 releases 和 snapshots 两种部署方式。

## 注意事项

1. **主题层级限制**：元数据主题最多支持 3 级
2. **快照管理**：Iceberg 表支持快照回滚和维护操作
3. **参数校验**：使用 `@Valid` 注解进行参数校验
4. **异常处理**：使用 `Assert` 工具类和 `SilentException`

## 作者

cy.Y (cyan-daimao)
