package com.cyan.dataman.application.bigdata.table.service.impl;

import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.application.bigdata.table.bo.assembler.TableMetaBOAssembler;
import com.cyan.dataman.application.bigdata.table.convert.TableMetaAppConvert;
import com.cyan.dataman.application.bigdata.table.service.TableMetaService;
import com.cyan.dataman.domain.bigdata.table.TableMeta;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaCmd;
import com.cyan.dataman.domain.bigdata.table.repository.TableMetaRepository;
import com.cyan.dataman.enums.DataLayer;
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
        TableMeta tableMeta = TableMetaAppConvert.INSTANCE.toTable(tableMetaCmd);
        tableMeta = tableMeta.save(tableMetaRepository);
        return TableMetaAppConvert.INSTANCE.toTableBO(tableMeta);
    }

    /**
     * 获取表列表
     *
     * @param db 数据库
     */
    @Override
    public List<TableMetaBO> listTableByDb(List<DataLayer> db) {
        List<TableMeta> tableMetas = tableMetaRepository.listTableByDb(db);
        return Optional.ofNullable(tableMetas).orElse(List.of()).stream().map(TableMetaAppConvert.INSTANCE::toTableBO).toList();
    }

    /**
     * 获取表
     *
     * @param db   库名
     * @param name 表名
     */
    @Override
    public TableMetaBO get(DataLayer db, String name) {
        TableMeta tableMeta = tableMetaRepository.get(db, name);
        return TableMetaAppConvert.INSTANCE.toTableBO(tableMeta);
    }

    /**
     * 获得表快照
     */
    @Override
    public TableMetaBO snapshots(DataLayer db, String name) {
        TableMetaBO tableMetaBO = get(db, name);
        tableMetaBOAssembler.assemblerSnapshots(tableMetaBO);
        return tableMetaBO;
    }
}

