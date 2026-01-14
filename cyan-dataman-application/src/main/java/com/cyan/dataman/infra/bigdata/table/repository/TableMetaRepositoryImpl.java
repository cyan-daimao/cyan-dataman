package com.cyan.dataman.infra.bigdata.table.repository;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.CollUtils;
import com.cyan.arch.common.util.Convert;
import com.cyan.arch.common.util.DateUtils;
import com.cyan.dataman.domain.bigdata.table.Field;
import com.cyan.dataman.domain.bigdata.table.TableMeta;
import com.cyan.dataman.domain.bigdata.table.TableSnapshot;
import com.cyan.dataman.domain.bigdata.table.repository.TableMetaRepository;
import com.cyan.dataman.enums.DataLayer;
import com.cyan.dataman.enums.PartitionType;
import com.cyan.dataman.enums.WriteMode;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.PartitionField;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.NoSuchNamespaceException;
import org.apache.iceberg.hive.HiveCatalog;
import org.apache.iceberg.types.Types;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author cy.Y
 * @version 1.0.0
 */
@Repository
@Slf4j
public class TableMetaRepositoryImpl implements TableMetaRepository {
    private final Catalog icebergCatalog;

    public TableMetaRepositoryImpl(Catalog icebergCatalog) {
        this.icebergCatalog = icebergCatalog;
    }

    /**
     * 获取表
     *
     * @param dbs 库列表
     * @return 表列表
     */
    @Override
    public List<TableMeta> listTableByDb(List<DataLayer> dbs) {
        if (CollUtils.isEmpty(dbs)) {
            return null;
        }

        // 1. 启动所有异步任务，每个返回 List<TableMeta>
        List<CompletableFuture<List<TableMeta>>> futures = dbs.stream()
                .map(db -> CompletableFuture.supplyAsync(() -> {
                    List<TableIdentifier> tableIdentifiers = icebergCatalog.listTables(Namespace.of(db.getCode()));
                    if (tableIdentifiers == null) {
                        return List.<TableMeta>of();
                    }
                    return tableIdentifiers.stream()
                            .map(ti -> new TableMeta().setDb(db).setName(ti.name()))
                            .collect(Collectors.toList());
                }))
                .toList();

        // 2. 等待全部完成，并合并结果
        return futures.stream()
                .map(CompletableFuture::join)   // 所有任务已完成，join() 不会阻塞
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 创建表
     *
     * @param tableMeta 表
     * @return 返回完整表信息
     */
    @Override
    public TableMeta save(TableMeta tableMeta) {
        TableIdentifier tableIdentifier = TableIdentifier.of(Namespace.of(tableMeta.getDb().getCode()), tableMeta.getName());
        Assert.isTrue(icebergCatalog.tableExists(tableIdentifier), new SilentException("表已存在"));
        //构建schema
        Assert.isTrue(CollUtils.isEmpty(tableMeta.getFields()), new SilentException("字段不能为空"));
        //构建字段
        List<Types.NestedField> fields = new ArrayList<>();
        for (int i = 0; i < tableMeta.getFields().size(); i++) {
            Field field = tableMeta.getFields().get(i);
            if (field.getRequired()) {
                fields.add(Types.NestedField.required(i, field.getName(), field.getTypes(), field.getComment()));
            } else {
                fields.add(Types.NestedField.optional(i, field.getName(), field.getTypes(), field.getComment()));
            }
        }
        Schema schema = new Schema(fields);
        //分区字段
        PartitionSpec.Builder partitionSpecBuilder = PartitionSpec.builderFor(schema);
        for (Field field : tableMeta.getFields()) {
            field.toIcebergPartitionSpecBuilder(partitionSpecBuilder);
        }
        Map<String, String> tableProperties = new HashMap<>();
        tableProperties.put("comment", tableMeta.getComment());
        Namespace namespace = Namespace.of(tableMeta.getDb().getCode());

        HiveCatalog hiveCatalog = (HiveCatalog) icebergCatalog;
        try {
            //  检查命名空间是否存在（通过尝试加载命名空间来验证）
            hiveCatalog.loadNamespaceMetadata(namespace);
        } catch (NoSuchNamespaceException e) {
            //  如果命名空间不存在，则创建它
            log.info("命名空间 {} 不存在，正在创建...", namespace);
            hiveCatalog.createNamespace(namespace);
        }

        org.apache.iceberg.Table iceTable = icebergCatalog.createTable(tableIdentifier, schema, partitionSpecBuilder.build(), tableProperties);
        return toTable(iceTable);
    }

    /**
     * 获取表
     *
     * @param db   库
     * @param name 表名
     * @return 表
     */
    @Override
    public TableMeta get(DataLayer db, String name) {
        TableIdentifier tableIdentifier = TableIdentifier.of(db.getCode(), name);
        org.apache.iceberg.Table iceTable = icebergCatalog.loadTable(tableIdentifier);
        return toTable(iceTable);
    }

    /**
     * 获得表快照
     */
    @Override
    public List<TableSnapshot> snapshots(DataLayer db, String name) {
        TableIdentifier tableIdentifier = TableIdentifier.of(db.getCode(), name);
        Table table = icebergCatalog.loadTable(tableIdentifier);
        List<TableSnapshot> snapshots = new ArrayList<>();
        table.snapshots().forEach(snapshot -> {
            TableSnapshot tableSnapshot = new TableSnapshot()
                    .setId(snapshot.snapshotId() + "")
                    .setWriteMode(WriteMode.getByCode(snapshot.operation()))
                    .setCreatedAt(DateUtils.toLocalDateTime(snapshot.timestampMillis()))
                    .setTotal(Convert.toLong(snapshot.summary().get("total-records")));
            snapshots.add(tableSnapshot);
        });
        return snapshots;
    }

    /**
     * iceberg table 转换成 table
     */
    private TableMeta toTable(org.apache.iceberg.Table iceTable) {
        Schema schema = iceTable.schema();
        List<Field> fields = Optional.ofNullable(schema.columns()).orElse(
                List.of()).stream().map(column -> new Field()
                .setName(column.name())
                .setType(Field.toFieldType(column.type()))
                .setComment(column.doc())).toList();
        String[] nameArr = iceTable.name().split("\\.");
        Map<String, String> properties = iceTable.properties();
        PartitionSpec spec = iceTable.spec();

        Map<String, PartitionField> ptMap = Optional.ofNullable(spec.fields()).orElse(List.of()).stream().collect(Collectors.toMap(p -> schema.findField(p.sourceId()).name(), p -> p));
        //  给字段设置分区字段
        for (Field field : fields) {
            PartitionField pt = ptMap.get(field.getName());
            if (pt != null) {
                Field.Partition partition = new Field.Partition(PartitionType.getByCode(pt.transform().toString()), null);
                field.setPt(partition);
            }
        }
        return new TableMeta()
                .setName(nameArr[2])
                .setDb(DataLayer.getByCode(nameArr[1]))
                .setCatalog(nameArr[0])
                .setComment(properties.get("comment"))
                .setFullName(iceTable.name())
                .setLocation(iceTable.location())
                .setFields(fields);
    }

}
