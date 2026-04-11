# 功能需求：数据库表结构同步与 CDC 感知

## 1. 背景与目标
我们已经开发了数据源的配置，并支持表结构编辑。现在我们需要支持用户选择表进行数据同步cdc同步到iceberg中。 需要你整合cyan-cdc的项目代码到cyan-dataman的cdc模块下
注意代码风格保持一致

我们一期的目标为：
1. 用户选择表进行数据同步cdc同步到iceberg中。
2. 创建debezium连接器绑定对应的数据源和库表

## 2. 用户故事
-   **作为** 后端开发人员
-   **我希望** 新增一个模块cdc的领域在四层结构中编写对应的接口
-   **以便于** 开发模块实现cdc的配置项接口

## 3. 核心功能流程
1.  **cdc配置管理**：
    - 支持cdc配置管理
    - 用户选择一个iceberg表时，如果他是flink同步必须检查iceberg表是否是ods层的表且必须存在op字段，如果不存在op字段需要创建该字段。



## 4. 数据结构设计建议
需要在现有系统中增加以下配置实体：
-   `cdc_config`: 数据源的配置信息，ds_config表的name(字段), db(字段), tbl(字段) 确定表的唯一标识。再定义iceberg的目标表名(字段). 可以参考cyan-cdc的表设计

## 5. 接口需求
-   `POST /api/v1/cdc`: 添加cdc配置
-   `GET /api/v1/cdc`: 获得cdc列表
-   `GET /api/v1/cdc/{cdcName}`: 获得cdc信息
-   `PUT /api/v1/cdc/{cdcName}`: 修改cdc配置
-   `PUT /api/v1/cdc/{cdcName}/open`: cdc开启和关闭
-   `DELETE /api/v1/cdc/{cdcName}`: 删除cdc

