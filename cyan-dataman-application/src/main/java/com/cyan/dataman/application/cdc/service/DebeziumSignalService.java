package com.cyan.dataman.application.cdc.service;

/**
 * Debezium 信号服务
 * 用于向源数据库发送增量快照信号
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface DebeziumSignalService {

    /**
     * 发送增量快照信号
     * 向源数据库的信号表插入一条记录，触发指定表的全量数据同步
     *
     * @param hostname  数据库主机地址
     * @param port      数据库端口
     * @param username  数据库用户名
     * @param password  数据库密码
     * @param dataTable 要执行快照的表，格式：db.table
     * @return 是否发送成功
     */
    boolean sendIncrementalSnapshotSignal(String hostname, String port, String username, String password, String dataTable);

    /**
     * 发送增量快照信号（多个表）
     */
    boolean sendIncrementalSnapshotSignal(String hostname, String port, String username, String password, String[] dataTables);

    /**
     * 确保信号表存在
     */
    boolean ensureSignalTableExists(String hostname, String port, String username, String password);
}
