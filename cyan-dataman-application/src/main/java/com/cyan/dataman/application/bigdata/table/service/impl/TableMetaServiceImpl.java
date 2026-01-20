package com.cyan.dataman.application.bigdata.table.service.impl;

import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.application.bigdata.table.bo.assembler.TableMetaBOAssembler;
import com.cyan.dataman.application.bigdata.table.convert.TableMetaAppConvert;
import com.cyan.dataman.application.bigdata.table.service.TableMetaService;
import com.cyan.dataman.domain.bigdata.table.TableMeta;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaCmd;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaDeleteCmd;
import com.cyan.dataman.domain.bigdata.table.repository.TableMetaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 表服务
 *
 * @author cy.Y
 * @version 1.0.0
 */
@Service
public class TableMetaServiceImpl implements TableMetaService {
    private final TableMetaRepository tableMetaRepository;
    private final TableMetaBOAssembler tableMetaBOAssembler;


    public TableMetaServiceImpl(TableMetaRepository tableMetaRepository, TableMetaBOAssembler tableMetaBOAssembler) {
        this.tableMetaRepository = tableMetaRepository;
        this.tableMetaBOAssembler = tableMetaBOAssembler;
    }


    /**
     * 创建表
     *
     * @param tableMetaCmd 创建表命令
     */
    @Override
    public TableMetaBO create(TableMetaCmd tableMetaCmd) {
        //转领域
        TableMeta tableMeta = tableMetaRepository.get(tableMetaCmd.getCatalog(), tableMetaCmd.getDb(), tableMetaCmd.getTbl());
        if (tableMeta != null) {
            throw new SilentException("表已存在");
        }
        tableMeta = TableMetaAppConvert.INSTANCE.toTable(tableMetaCmd);
        tableMeta = tableMetaRepository.save(tableMeta);
        return TableMetaAppConvert.INSTANCE.toTableBO(tableMeta);
    }


    /**
     * 获取表列表
     *
     * @param catalog 目录
     * @param db      数据库
     */
    @Override
    public List<TableMetaBO> listTableByDb(String catalog, List<String> db) {
        List<TableMeta> tableMetas = tableMetaRepository.listTableByDb(catalog, db);
        return Optional.ofNullable(tableMetas).orElse(List.of()).stream().map(TableMetaAppConvert.INSTANCE::toTableBO).toList();
    }

    /**
     * 获取表
     *
     * @param db   库名
     * @param name 表名
     */
    @Override
    public TableMetaBO get(String catalog, String db, String name) {
        TableMeta tableMeta = tableMetaRepository.get(catalog, db, name);
        return TableMetaAppConvert.INSTANCE.toTableBO(tableMeta);
    }

    /**
     * 获得表快照
     */
    @Override
    public TableMetaBO snapshots(String catalog, String db, String name) {
        TableMetaBO tableMetaBO = get(catalog, db, name);
        tableMetaBOAssembler.assemblerSnapshots(tableMetaBO);
        return tableMetaBO;
    }

    /**
     * 删除表
     *
     * @param cmd 删除表命令
     */
    @Override
    public void delete(TableMetaDeleteCmd cmd) {
        tableMetaRepository.delete(cmd.getCatalog(),cmd.getDb(),cmd.getTbl());
    }
}

