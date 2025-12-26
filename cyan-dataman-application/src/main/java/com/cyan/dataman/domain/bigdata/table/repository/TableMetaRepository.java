package com.cyan.dataman.domain.bigdata.table.repository;

import com.cyan.dataman.domain.bigdata.table.TableMeta;
import com.cyan.dataman.domain.bigdata.table.TableSnapshot;
import com.cyan.dataman.enums.DataLayer;

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
     * @param dbs 库列表
     * @return 表列表
     */
    List<TableMeta> listTableByDb(List<DataLayer> dbs);

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
     * @param db   库
     * @param name 表名
     * @return 表
     */
    TableMeta get(DataLayer db, String name);

    /**
     * 获取表快照数据
     *
     * @param db   库
     * @param name 表名
     * @return 表快照数据
     */
    List<TableSnapshot> snapshots(DataLayer db, String name);
}
