package com.cyan.dataman.application.bigdata.table.service;

import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaCmd;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaDeleteCmd;

import java.util.List;

/**
 * 数据湖表服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface TableMetaService {
    /**
     * 创建表
     *
     * @param tableMetaCmd 创建表命令
     * @return 创建表结果
     */
    TableMetaBO create(TableMetaCmd tableMetaCmd);

    /**
     * 获取表列表
     *
     * @param catalog 目录
     * @param db 库名
     */
    List<TableMetaBO> listTableByDb(String catalog,List<String> db);

    /**
     * 获取表
     *
     * @param catalog 目录
     * @param db   库名
     * @param name 表名
     */
    TableMetaBO get(String catalog, String db, String name);

    /**
     * 获取表快照数据
     *
     * @param db   库名
     * @param name 表名
     */
    TableMetaBO snapshots(String catalog, String db, String name);

    /**
     * 删除表
     *
     * @param cmd 删除表命令
     */
    void delete(TableMetaDeleteCmd cmd);
}
