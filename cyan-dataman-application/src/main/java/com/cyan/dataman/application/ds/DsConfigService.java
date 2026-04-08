package com.cyan.dataman.application.ds;

import com.cyan.dataman.application.ds.bo.DsConfigBO;
import com.cyan.dataman.application.ds.cmd.DatabaseCreateCmd;
import com.cyan.dataman.application.ds.cmd.DsConfigCmd;
import com.cyan.dataman.application.ds.cmd.TableSchemaCmd;
import com.cyan.dataman.domain.ds.query.DsConfigListQuery;
import com.cyan.dataman.domain.ds.valobj.DatabaseValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.enums.DatasourceType;

import java.util.List;
import java.util.Map;

/**
 * 数据源配置服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface DsConfigService {

    /**
     * 创建数据源配置
     */
    DsConfigBO create(DsConfigCmd cmd);

    /**
     * 获取数据源配置列表
     */
    List<DsConfigBO> list(DsConfigListQuery query);

    /**
     * 根据id获取数据源配置
     */
    DsConfigBO findById(String id);

    /**
     * 通过名称获取数据源配置
     */
    DsConfigBO findByName(String ds);

    /**
     * 更新数据源配置
     */
    DsConfigBO update(String dsName, DsConfigCmd cmd);

    /**
     * 删除数据源配置
     */
    void delete(String dsName);

    /**
     * 测试数据源连接
     */
    void testConnection(String dsName);

    /**
     * 获取数据源类型
     */
    DatasourceType getDatasourceType(String dsName);

    /**
     * 获取数据库列表
     */
    List<DatabaseValObj> listDatabases(String dsName);

    /**
     * 创建数据库
     */
    void createDatabase(String dsName, DatabaseCreateCmd cmd);

    /**
     * 获取表列表（包含表名和注释）
     */
    List<TableSchemaValObj> listTables(String dsName, String dbName);

    /**
     * 获取表结构详情
     */
    TableSchemaValObj getTableSchema(String dsName, String dbName, String tableName);

    /**
     * 创建表
     */
    void createTable(String dsName, String dbName, TableSchemaCmd cmd);

    /**
     * 更新表结构
     */
    void updateTable(String dsName, String dbName, String tableName, TableSchemaCmd cmd);

    /**
     * 删除表
     */
    void dropTable(String dsName, String dbName, String tableName);

    /**
     * 执行 SQL 语句（自动判断 DQL 或 DML）
     * @param dsName 数据源名称
     * @param dbName 数据库名
     * @param sql SQL语句
     * @param limit 结果行数限制（仅查询有效），null表示不限制
     * @return 执行结果
     */
    Map<String, Object> executeSql(String dsName, String dbName, String sql, Integer limit);
}
