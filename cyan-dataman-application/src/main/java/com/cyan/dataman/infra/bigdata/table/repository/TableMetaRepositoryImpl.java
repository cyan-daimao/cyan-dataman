package com.cyan.dataman.infra.bigdata.table.repository;

import com.cyan.arch.common.util.CollUtils;
import com.cyan.dataman.application.bigdata.table.convert.TableMetaAppConvert;
import com.cyan.dataman.domain.bigdata.table.TableMeta;
import com.cyan.dataman.domain.bigdata.table.TableSnapshot;
import com.cyan.dataman.domain.bigdata.table.repository.TableMetaRepository;
import com.cyan.dataman.infra.util.GravitinoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.gravitino.NameIdentifier;
import org.apache.gravitino.Namespace;
import org.apache.gravitino.SupportsSchemas;
import org.apache.gravitino.client.GravitinoClient;
import org.apache.gravitino.rel.TableCatalog;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * @author cy.Y
 * @version 1.0.0
 */
@Repository
@Slf4j
public class TableMetaRepositoryImpl implements TableMetaRepository {
    private final GravitinoClient gravitinoClient;

    public TableMetaRepositoryImpl(GravitinoClient gravitinoClient) {
        this.gravitinoClient = gravitinoClient;
    }

    /**
     * 获取表
     *
     * @param catalog 目录
     * @param dbs     传null或空数组则获取所有数据库
     * @return 表列表
     */
    @Override
    public List<TableMeta> listTableByDb(String catalog, List<String> dbs) {
        org.apache.gravitino.Catalog loadCatalog = gravitinoClient.loadCatalog(catalog);
        TableCatalog tableCatalog = loadCatalog.asTableCatalog();
        NameIdentifier[] nameIdentifiers = null;
        if (CollUtils.isEmpty(dbs)) {
            SupportsSchemas schemas = loadCatalog.asSchemas();
            String[] schema = schemas.listSchemas();
            nameIdentifiers = tableCatalog.listTables(Namespace.of(schema));
        } else {
            tableCatalog.listTables(Namespace.of(dbs.toArray(String[]::new)));
        }
        if (nameIdentifiers == null) {
            return Collections.emptyList();
        }

        //使用虚拟线程并发
        List<CompletableFuture<TableMeta>> futures = Arrays.stream(nameIdentifiers).map(nameIdentifier -> CompletableFuture.supplyAsync(() -> {
            org.apache.gravitino.rel.Table table = tableCatalog.loadTable(nameIdentifier);
            String[] name = nameIdentifier.name().split("\\.");
            return TableMetaAppConvert.INSTANCE.toTableMeta(table, catalog, name[0]);
        }, Executors.newVirtualThreadPerTaskExecutor())).toList();
        //join等待所有线程执行完毕
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * 创建表
     *
     * @param tableMeta 表
     * @return 返回完整表信息
     */
    @Override
    public TableMeta save(TableMeta tableMeta) {
        org.apache.gravitino.Catalog catalog = gravitinoClient.loadCatalog(tableMeta.getCatalog());
        NameIdentifier nameIdentifier = NameIdentifier.of(tableMeta.getDb(), tableMeta.getTbl());
        TableCatalog tableCatalog = catalog.asTableCatalog();
        org.apache.gravitino.rel.Table table = tableCatalog.createTable(nameIdentifier, GravitinoUtil.toColumns(tableMeta.getFields()), tableMeta.getComment(), null);
        return TableMetaAppConvert.INSTANCE.toTableMeta(table, tableMeta.getDb(), tableMeta.getTbl());
    }

    /**
     * 获取表
     *
     * @param catalog 目录
     * @param db      库
     * @param tbl     表名
     * @return 表
     */
    @Override
    public TableMeta get(String catalog, String db, String tbl) {
        NameIdentifier nameIdentifier = NameIdentifier.of(db, tbl);
        TableCatalog tableCatalog = gravitinoClient.loadCatalog(catalog).asTableCatalog();
        boolean exists = tableCatalog.tableExists(nameIdentifier);
        if (exists) {
            org.apache.gravitino.rel.Table table = tableCatalog.loadTable(nameIdentifier);
            return TableMetaAppConvert.INSTANCE.toTableMeta(table, db, tbl);
        }
        return null;
    }

    /**
     * 获取表快照数据
     *
     * @param db   库
     * @param name 表名
     * @return 表快照数据
     */
    @Override
    public List<TableSnapshot> snapshots(String db, String name) {
        return List.of();
    }


    /**
     * 删除表
     *
     * @param catalog 目录
     * @param db      库
     * @param tbl     表名
     */
    @Override
    public void delete(String catalog, String db, String tbl) {
        TableCatalog tableCatalog = gravitinoClient.loadCatalog(catalog).asTableCatalog();
        tableCatalog.dropTable(NameIdentifier.of(db, tbl));
    }

}
