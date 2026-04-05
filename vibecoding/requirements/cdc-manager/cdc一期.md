# 功能需求：数据库表结构同步与 CDC 感知

## 1. 背景与目标
我们已经开发了数据源的配置，并支持表结构编辑。现在我们需要支持用户选择表进行数据同步cdc同步到iceberg中。
我们一期的目标为：
1. 用户选择表进行数据同步cdc同步到iceberg中。
2. 使用spark根据数据表的updated_at字段进行数据同步。

## 2. 用户故事
-   **作为** 后端开发人员
-   **我希望** 新增一个模块cdc的领域在四层结构中编写对应的接口
-   **以便于** 开发模块实现cdc的配置项接口

## 3. 核心功能流程
1.  **cdc配置管理**：
    -   支持cdc配置管理
2.  **spark** ：
    - 为每一个spark配置(区分覆盖和追加)写spark-sql: insert into ${iceberg_table_name} select * from ${source_table_name}} 这个sql要记录到表里
    - 写一个event用来提交sparksql和开启spark任务关闭spark任务
    - 记录spark的运行状态， 运行日志，运行结果 ，运行时间等信息

## 4. 数据结构设计建议
需要在现有系统中增加以下配置实体：
-   `cdc_config`: 数据源的配置信息，ds_config表的name(字段), db(字段), tbl(字段) 确定表的唯一标识。再定义iceberg的目标表名(字段). 同步工具(字段): spark,flink,同步sql(字段)
-   `cdc_spark_job`: cdc_config 的spark的配置信息,调度信息
-   `cdc_spark_task`: 实例
-   `cdc_spark_task_log`: spark任务日志

## 5. 接口需求
-   `POST /api/v1/cdc`: 添加cdc配置
-   `GET /api/v1/cdc`: 获得cdc列表
-   `GET /api/v1/cdc/{cdcName}`: 获得cdc信息
-   `PUT /api/v1/cdc/{cdcName}`: 修改cdc配置
-   `PUT /api/v1/cdc/{cdcName}/open`: cdc开启和关闭
-   `DELETE /api/v1/cdc/{cdcName}`: 删除cdc

