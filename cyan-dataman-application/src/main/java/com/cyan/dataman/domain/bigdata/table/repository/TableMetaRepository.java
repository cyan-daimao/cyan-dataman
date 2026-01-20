package com.cyan.dataman.domain.bigdata.table.repository;

import com.cyan.dataman.domain.bigdata.table.TableMeta;
import com.cyan.dataman.domain.bigdata.table.TableSnapshot;

import java.util.List;

/**
 * 表仓储层
 *
 * @author cy.Y
 * @version 1.0.0
 */
public interface TableMetaRepository {

    /**
     * 获取表
     *
     * @param catalog 目录
     * @param dbs 库列表
     * @return 表列表
     */
    List<TableMeta> listTableByDb(String catalog,List<String> dbs);

    /**
     * 创建表
     *
     * @param tableMeta 表
     * @return 返回完整表信息
     */
    TableMeta save(TableMeta tableMeta);

    /**
     * 获取表
     *
     * @param catalog 目录
     * @param db   库
     * @param tbl 表名
     * @return 表
     */
    TableMeta get(String catalog, String db, String tbl);

    /**
     * 获取表快照数据
     *
     * @param db   库
     * @param name 表名
     * @return 表快照数据
     */
    List<TableSnapshot> snapshots(String db, String name);

    /**
     * 删除表
     *
     * @param catalog 目录
     * @param db   库
     * @param tbl 表名
     */
    void delete(String catalog, String db, String tbl);
}
