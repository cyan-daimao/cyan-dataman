# CDC 三期方案：ODS 统一表 + 主题关联

## 一、方案概述

采用**方案 B（ODS 统一表）**，并进行产品层面重构：

1. **CDC 数据写入统一的 ODS 表** `ods_cdc_raw_{dsName}`，不再写入用户指定的目标 Iceberg 表
2. **CDC 配置入口迁移**：从「元数据表详情页」移到「业务数据库 → 表结构管理」
3. **主题强关联**：CDC 任务必须关联主题，主题 code 作为 Iceberg ODS 表名前缀
4. **前置校验**：创建 CDC 任务时，自动检查是否存在 `subjectCode = "cdc"` 的主题，不存在则禁止创建

---

## 二、核心变更点

### 2.1 数据模型变更

#### `cdc_config` 表

| 变更 | 字段 | 说明 |
|------|------|------|
| **新增** | `subject_code` VARCHAR(64) | 关联的主题编码，ODS 表前缀 |
| **废弃** | `iceberg_table_name` | 方案 B 不再需要用户指定目标表，改为系统生成 |

> `iceberg_table_name` 暂不删除，保留兼容，但前端不再填写，后端不再使用。

#### ODS 表命名规则

```
ods_cdc_raw_{subjectCode}_{dsName}
```

例如：
- 主题 `cdc` + 数据源 `mysql-x99` → `ods_cdc_raw_cdc_mysql_x99`
- 主题 `order` + 数据源 `pg-main` → `ods_cdc_raw_order_pg_main`

> 主题作为前缀是为了满足「主题是表名的前缀」这一业务规则。

---

### 2.2 后端变更

#### 1) 领域层 — `CdcConfig.java`

```java
// 新增字段
private String subjectCode;

// 废弃字段（保留兼容，validate 不再校验）
// private String icebergTableName;
```

#### 2) 应用层 — `CdcConfigServiceImpl.java`

**创建 CDC 配置时新增校验逻辑：**

```java
@Override
@Transactional
public CdcConfigBO create(CdcConfigCmd cmd) {
    // ... 原有校验 ...

    // 【新增】校验主题
    validateSubject(cmd.getSubjectCode());

    // 【废弃】不再调用 ensureOpColumnForFlinkCdc
    // 方案 B：数据写入系统管理的 ODS 统一表，不写入用户目标表

    // ... 原有逻辑 ...
}

private void validateSubject(String subjectCode) {
    Assert.notBlank(subjectCode, new SilentException("主题编码不能为空"));

    // 自动兜底：如果前端没传，尝试查找 code = "cdc" 的主题
    if (subjectCode == null) {
        MetadataSubject cdcSubject = metadataSubjectRepository
            .findOne(new MetadataSubjectFindQuery().setSubjectCode("cdc"));
        Assert.notNull(cdcSubject, new SilentException(
            "未找到 code 为 cdc 的主题，请先创建主题或选择其他主题"
        ));
        subjectCode = cdcSubject.getSubjectCode();
    }

    // 校验主题是否存在
    MetadataSubject subject = metadataSubjectRepository
        .findOne(new MetadataSubjectFindQuery().setSubjectCode(subjectCode));
    Assert.notNull(subject, new SilentException("主题不存在: " + subjectCode));
}
```

**废弃 `ensureOpColumnForFlinkCdc` 方法**

方案 B 中 ODS 表由 Flink SQL `CREATE TABLE IF NOT EXISTS` 自动创建并管理，不需要预先检查/修改用户目标表。

#### 3) Flink SQL 层 — `CdcFlinkSyncServiceImpl.java`

**修改 `buildFlinkSql`：**

```java
private String buildFlinkSql(String dsName, String subjectCode) {
    String safeDsName = dsName.replaceAll("[^a-zA-Z0-9_]", "_");
    String safeSubject = subjectCode.replaceAll("[^a-zA-Z0-9_]", "_");
    String odsTableName = "ods_cdc_raw_" + safeSubject + "_" + safeDsName;

    return String.format("""
        CREATE TABLE IF NOT EXISTS kafka_cdc_%s (
          _raw_json STRING
        ) WITH (
          'connector' = 'kafka',
          'topic' = 'cdc-%s.*',
          'properties.bootstrap.servers' = '%s',
          'properties.group.id' = 'flink-cdc-ods-%s',
          'scan.startup.mode' = 'earliest-offset',
          'format' = 'raw'
        );

        CREATE TABLE IF NOT EXISTS %s (
          _raw_json STRING,
          _op STRING,
          _ts BIGINT,
          _db STRING,
          _table STRING,
          _ingestion_time TIMESTAMP_LTZ(3)
        ) WITH (
          'connector' = 'iceberg',
          'catalog-name' = 'rest',
          'catalog-type' = 'rest',
          'uri' = '%s',
          'warehouse' = 's3://lakehouse/ods',
          'format-version' = '2',
          'write.format.default' = 'parquet',
          'write.upsert.enabled' = 'false'
        );

        INSERT INTO %s SELECT
          _raw_json,
          COALESCE(JSON_VALUE(_raw_json,'$.payload.op'),JSON_VALUE(_raw_json,'$.op')) AS _op,
          CAST(COALESCE(JSON_VALUE(_raw_json,'$.payload.ts_ms'),JSON_VALUE(_raw_json,'$.ts_ms')) AS BIGINT) AS _ts,
          COALESCE(JSON_VALUE(_raw_json,'$.payload.source.db'),JSON_VALUE(_raw_json,'$.source.db')) AS _db,
          COALESCE(JSON_VALUE(_raw_json,'$.payload.source.table'),JSON_VALUE(_raw_json,'$.source.table')) AS _table,
          NOW() AS _ingestion_time
        FROM kafka_cdc_%s;
        """, safeDsName, safeDsName, kafkaBootstrapServers, safeDsName,
            odsTableName, icebergRestUri,
            odsTableName, safeDsName);
}
```

**关键变更：**
- Sink 表名从 `ods_cdc_raw_{dsName}` 改为 `ods_cdc_raw_{subjectCode}_{dsName}`
- 方法签名增加 `subjectCode` 参数
- 保持 `CREATE TABLE IF NOT EXISTS`（ODS 表首次创建，后续复用）

#### 4) 主题 Repository — 新增按 code 查询

```java
// MetadataSubjectRepository.java 接口新增
MetadataSubject findOne(MetadataSubjectFindQuery query);

// MetadataSubjectRepositoryImpl.java 实现
@Override
public MetadataSubject findOne(MetadataSubjectFindQuery query) {
    LambdaQueryWrapper<MetadataSubjectDO> wrapper = new LambdaQueryWrapper<>();
    if (StrUtils.isNotBlank(query.getSubjectCode())) {
        wrapper.eq(MetadataSubjectDO::getSubjectCode, query.getSubjectCode());
    }
    if (StrUtils.isNotBlank(query.getSubjectName())) {
        wrapper.eq(MetadataSubjectDO::getSubjectName, query.getSubjectName());
    }
    wrapper.last("LIMIT 1");
    MetadataSubjectDO subjectDO = baseMapper.selectOne(wrapper);
    return subjectDO == null ? null : MetadataSubjectInfraConvert.INSTANCE.toDomain(subjectDO);
}
```

#### 5) DB 迁移脚本

```sql
-- V1.0.2__cdc_subject_code.sql
ALTER TABLE cdc_config ADD COLUMN subject_code VARCHAR(64) AFTER table_name;
CREATE INDEX idx_subject_code ON cdc_config(subject_code);
```

---

### 2.3 前端变更

#### 1) 移除元数据表详情页的 CDC Tab

**文件：** `src/pages/metadata/metadata_table/detail/AsyncJob.tsx`

- 移除「CDC 同步」tab（保留 SQL 同步和手动上传）
- 移除 CDC 相关状态、方法、模态框
- 或改为只读展示（如果需要）

#### 2) 在「业务数据库 → 表结构管理」添加 CDC 入口

**文件：** `src/pages/metadata/business_db/table_schema/index.tsx`

**操作列增加「CDC 同步」按钮：**

```tsx
// 在 columns 的 action render 中新增
<Tooltip title="CDC 同步">
    <Button
        type="text"
        size="small"
        icon={<CloudSyncOutlined />}
        onClick={() => handleOpenCdcModal(record.tableName)}
        style={{ color: '#722ED1' }}
    />
</Tooltip>
```

**新增 CDC 配置模态框：**

表单字段：
- 数据源：`selectedDsName`（只读，页面已选）
- 数据库：`selectedDbName`（只读，页面已选）
- 表名：`tableName`（只读，当前行）
- **主题选择**：Dropdown，调 `/api/v1/metadata/subjects` 获取主题列表
  - 如果存在 `subjectCode = "cdc"` 的主题，默认选中
  - 如果不存在，提示「未找到 cdc 主题，请先创建」并禁用提交
- 同步模式：`full` / `incremental`

**CDC 状态联动：**

```tsx
// 加载表列表时，同时查询 CDC 配置状态
const fetchTables = async (dsName: string, dbName: string) => {
    const [tablesRes, cdcRes] = await Promise.all([
        tableApi.list(dsName, dbName),
        listCdcConfigs({ dsName, syncTool: 'FLINK' })  // 新增查询
    ]);

    const cdcMap = new Map(cdcRes.data?.map(c => [`${c.dbName}.${c.tableName}`, c]));

    const tableInfos = tablesRes.data.map(tbl => ({
        tableName: tbl.tableName,
        tableComment: tbl.tableComment,
        cdcEnabled: cdcMap.has(`${dbName}.${tbl.tableName}`),
        cdcConfigId: cdcMap.get(`${dbName}.${tbl.tableName}`)?.id,
        envStatus: EnvStatus.SYNCED,
    }));
};
```

#### 3) `CdcApi.ts` 调整

```ts
export interface CdcConfigCmd {
    name: string;
    dsName: string;
    dbName: string;
    tableName: string;
    subjectCode: string;        // 【新增】主题编码
    // icebergTableName: string; // 【废弃】方案 B 不再需要
    syncTool: 'SPARK' | 'FLINK';
    syncMode?: 'full' | 'incremental';
    description?: string;
}
```

#### 4) 路由调整

无需新增路由，CDC 配置通过「业务数据库 → 表结构管理」页面内的模态框完成。

---

## 三、交互流程

```
用户进入「业务数据库 → 表结构管理」
  → 选择数据源 + 数据库
    → 表列表加载
      → 每行显示 CDC 状态（已启用/未启用）
        → 点击「CDC 同步」按钮
          → 弹出 CDC 配置模态框
            → 自动填充 dsName / dbName / tableName
            → 调用 /api/v1/metadata/subjects 获取主题列表
              → 如果存在 code=cdc 的主题 → 默认选中
              → 如果不存在 → 提示错误，禁用提交
            → 用户选择主题 + 同步模式
            → 提交 → POST /api/v1/cdc
              → 后端校验主题存在
              → 保存 CDC 配置
              → 如果启用 → 启动 Debezium Connector + Flink SQL 作业
                → Flink SQL 自动创建 ODS 表（如果不存在）
                  → 数据写入 ods_cdc_raw_{subjectCode}_{dsName}
```

---

## 四、ODS 表生命周期

| 场景 | 行为 |
|------|------|
| 首次创建 CDC 配置 | Flink SQL `CREATE TABLE IF NOT EXISTS` 自动创建 ODS 表 |
| 同一数据源新增表 | 复用同一 ODS 表（`_db` + `_table` 字段区分来源） |
| ODS 表已存在 | `IF NOT EXISTS` 跳过创建，直接 `INSERT INTO` |
| 删除所有 CDC 配置 | ODS 表保留（历史数据不删除） |
| 表结构变更 | ODS 表 Schema 固定（`_raw_json` 保留完整 JSON），无需变更 |

---

## 五、边界情况处理

### 5.1 没有 cdc 主题

- 前端：模态框主题下拉显示「未找到 cdc 主题」，提交按钮禁用
- 后端：`validateSubject` 校验失败，抛 `SilentException`

### 5.2 ODS 表已存在但 Schema 不匹配

- 风险：如果之前手动创建了同名 ODS 表，字段不匹配会导致 INSERT 失败
- 处理：Flink SQL 报错后，作业状态变为 FAILED，通过 `CdcFlinkJob.errorMessage` 展示错误
- 后续优化：可在提交 Flink SQL 前通过 Iceberg REST API 检查表结构（二期）

### 5.3 主题 code 包含非法字符

- `buildFlinkSql` 中对 `subjectCode` 做 `replaceAll("[^a-zA-Z0-9_]", "_")` 处理

### 5.4 Spark CDC 兼容性

- Spark CDC（如果有）不受影响，继续使用 `icebergTableName`
- 只有 FLINK CDC 走 ODS 统一表

---

## 六、任务清单

### 后端

- [ ] `CdcConfig` / `CdcConfigDO` / `CdcConfigDTO` / `CdcConfigCmd` / `CdcConfigBO` 添加 `subjectCode`
- [ ] `CdcConfigServiceImpl` 添加 `validateSubject` 校验
- [ ] `CdcConfigServiceImpl` 废弃 `ensureOpColumnForFlinkCdc`
- [ ] `CdcFlinkSyncServiceImpl.buildFlinkSql` 增加 `subjectCode` 参数，修改 ODS 表命名
- [ ] `MetadataSubjectRepository` 新增 `findOne(query)` 方法
- [ ] DB 迁移脚本 `V1.0.2__cdc_subject_code.sql`

### 前端

- [ ] `AsyncJob.tsx` 移除 CDC 同步 tab
- [ ] `table_schema/index.tsx` 操作列增加 CDC 同步按钮
- [ ] `table_schema/index.tsx` 加载表列表时联动查询 CDC 状态
- [ ] 新增 `CdcConfigModal` 组件（或内联在 index.tsx）
- [ ] `CdcApi.ts` 调整 `CdcConfigCmd` 接口
- [ ] 主题 API 调用（已有 `MetadataSubjectAPI.ts`）

---

## 七、影响范围

| 模块 | 影响 | 备注 |
|------|------|------|
| CDC 配置创建 | 新增 `subjectCode` 必填 | 前端+后端 |
| CDC 配置列表 | 展示 `subjectCode` | 后端 |
| Flink SQL 生成 | ODS 表名含主题前缀 | 后端 |
| 元数据表详情页 | 移除 CDC tab | 前端 |
| 业务数据库表管理 | 新增 CDC 入口 | 前端 |
| 主题管理 | 无变更 | 已有功能 |
| Spark CDC | 无影响 | 独立逻辑 |
