# 功能需求：数据库表结构同步与 CDC 感知

## 1. 背景与目标
我们已经实现了cdc的配置信息管理，现在需要flink去读取CdcConfig的信息然后去创建flink任务读取debezium的kafka

本期只需要实现flink


## 2. 核心功能流程
1.  **cdc配置管理**：
    - 创建com.cyan.dataman.domain.cdc.CdcConfig和修改cdcConfig时,用户选择一个iceberg表时，如果他是flink同步必须检查iceberg表是否是ods层的表且必须存在op字段，
     如果不存在op字段需要创建该字段。
2.  **flink任务创建**：
    - 使用com.cyan.dataman.infra.config.FlinkConfig的streamExecutionEnvironment执行flink任务
    - 查询com.cyan.dataman.domain.cdc.CdcConfig，筛选出enabled=true and syncTool = FLINK 的记录,要注意用户如果暂停或开启(enabled)需要开启和关闭对应flink任务
    - 将flink-cdc记录保存到com.cyan.dataman.infra.persistence.cdc.dos.CdcFlinkJobDO中
    - 通过CdcConfig表中的 kafka地址和topic名称去读取debezium的kafka数据
    - 使用flink的kafka connector去读取kafka数据
    - 使用flink的iceberg connector去写入iceberg表。
      插入iceberg表时，通过com.cyan.dataman.application.metadata.MetadataTableService的findOne获得iceberg表结构
      推荐不使用flinksql,因为我们要一个slot同步多个表，如果你非要使用flinksql，那么你需要考虑如何动态的进行某个表的同步暂停开启和删除
    - 要注意不可以一个topic一个任务因为会占用大量的slot,所以我们的cdc要使用同一个slot.你要考虑怎么实现动态的进行某个表的同步暂停开启和删除

3.  **CdcFlinkJobDO表的crud开发**：
    - 模仿其他领域来写CdcFlinkJobDO的crud，要求do,entity,bo,dto四层
     
## 3. 数据结构设计建议
    - 使用CdcFlinkJobDO表，你可以进行字段的优化

## 4. 接口需求
不需要rest接口

## 5. 规范
每个class的属性都要加上注释