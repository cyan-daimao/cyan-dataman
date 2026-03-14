package com.cyan.dataman.application.metadata.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.application.metadata.convert.MetadataTableAppConvert;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.enums.DatasourceType;
import org.apache.gravitino.Catalog;
import org.apache.gravitino.NameIdentifier;
import org.apache.gravitino.client.GravitinoClient;
import org.apache.gravitino.rel.Table;
import org.apache.gravitino.rel.TableCatalog;
import org.apache.gravitino.rel.TableChange;
import org.apache.gravitino.rel.types.Types;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 元数据服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class MetadataTableServiceImpl implements MetadataTableService {
    private final MetadataTableRepository metadataTableRepository;
    private final GravitinoClient gravitinoClient;

    public MetadataTableServiceImpl(MetadataTableRepository metadataTableRepository, GravitinoClient gravitinoClient) {
        this.metadataTableRepository = metadataTableRepository;
        this.gravitinoClient = gravitinoClient;
    }

    /**
     * 获取表列表
     */
    @Override
    public List<MetadataTableBO> list(MetadataTableListQuery query) {
        List<MetadataTable> tables = metadataTableRepository.list(query);
        return Optional.ofNullable(tables).orElse(List.of()).stream().map(MetadataTableAppConvert.INSTANCE::toMetadataTableBO).toList();
    }

    /**
     * 获取单查表
     */
    @Override
    public MetadataTableBO findOne(MetadataTableOneQuery query) {
        MetadataTable metadataTable = metadataTableRepository.findOne(query);
        return MetadataTableAppConvert.INSTANCE.toMetadataTableBO(metadataTable);
    }

    /**
     * 获取表
     */
    @Override
    public MetadataTableBO findById(String id) {
        MetadataTable metadataTable = metadataTableRepository.findById(id);
        return MetadataTableAppConvert.INSTANCE.toMetadataTableBO(metadataTable);
    }

    /**
     * 创建表
     */
    @Override
    public MetadataTableBO save(MetadataTableCmd cmd) {
        MetadataTableBO one = findOne(new MetadataTableOneQuery().setName(cmd.getName()));
        Assert.isTrue(one==null, new SilentException("表已存在"));
        MetadataTable metadataTable = MetadataTableAppConvert.INSTANCE.toMetadataTable(cmd);
        metadataTable.setDatasourceType(DatasourceType.ICEBERG);
        metadataTable.setLayerCode(cmd.getLayerCode().getCode().toLowerCase());
        metadataTable.getTable().setSchema(cmd.getLayerCode().getCode().toLowerCase());
        // 先在Gravitino中创建表
        createTableInGravitino(metadataTable);
        // 保存到数据库
        metadataTable = metadataTable.save(metadataTableRepository);
        return MetadataTableAppConvert.INSTANCE.toMetadataTableBO(metadataTable);
    }

    /**
     * 在Gravitino中创建表
     */
    private void createTableInGravitino(MetadataTable table) {
        Catalog catalog = gravitinoClient.loadCatalog("iceberg");
        TableCatalog tableCatalog = catalog.asTableCatalog();
        // 构建字段
        org.apache.gravitino.rel.Column[] columns = Optional.ofNullable(table.getTable().getColumns()).orElse(List.of())
                .stream()
                .map(this::toGravitinoColumn)
                .toArray(org.apache.gravitino.rel.Column[]::new);
        // 创建表
        tableCatalog.createTable(
                NameIdentifier.of(table.getTable().getSchema(), table.getTable().getName()),
                columns,
                table.getComment(),
                null
        );
    }

    /**
     * 转换为Gravitino字段
     */
    private org.apache.gravitino.rel.Column toGravitinoColumn(ColumnValObj columnValObj) {
        return org.apache.gravitino.rel.Column.of(
                columnValObj.getName(),
                Types.StringType.get(),
                columnValObj.getComment(),
                columnValObj.getNullable(),
                columnValObj.getAutoIncrement(),
                null
        );
    }

    /**
     * 更新表
     */
    @Override
    public MetadataTableBO update(String id, MetadataTableCmd cmd) {
        MetadataTable existingTable = metadataTableRepository.findById(id);
        if (existingTable == null) {
            throw new RuntimeException("表不存在");
        }
        MetadataTable metadataTable = MetadataTableAppConvert.INSTANCE.toMetadataTable(cmd);
        metadataTable.setId(id);
        metadataTable.setDatasourceType(DatasourceType.ICEBERG);
        metadataTable.setLayerCode(cmd.getLayerCode().getCode().toLowerCase());
        metadataTable.getTable().setSchema(cmd.getLayerCode().getCode().toLowerCase());
        // 在Gravitino中更新表
        updateTableInGravitino(existingTable, metadataTable);
        // 更新数据库
        metadataTable = metadataTable.update(metadataTableRepository);
        return MetadataTableAppConvert.INSTANCE.toMetadataTableBO(metadataTable);
    }

    /**
     * 在Gravitino中更新表
     */
    private void updateTableInGravitino(MetadataTable oldTable, MetadataTable newTable) {
        Catalog catalog = gravitinoClient.loadCatalog("iceberg");
        TableCatalog tableCatalog = catalog.asTableCatalog();
        NameIdentifier tableIdent = NameIdentifier.of(oldTable.getTable().getSchema(), oldTable.getTable().getName());

        List<TableChange> changes = new ArrayList<>();
        
        // 更新表注释
        if (!oldTable.getComment().equals(newTable.getComment())) {
            changes.add(TableChange.updateComment(newTable.getComment()));
        }

        // 获取旧表的字段信息
        Table existingGravitinoTable = tableCatalog.loadTable(tableIdent);
        Map<String, org.apache.gravitino.rel.Column> oldColumns = Arrays.stream(Optional.ofNullable(existingGravitinoTable.columns())
                .orElse(new org.apache.gravitino.rel.Column[0]))
                .collect(Collectors.toMap(org.apache.gravitino.rel.Column::name, Function.identity()));

        // 获取新表的字段信息
        Map<String, ColumnValObj> newColumns = Optional.ofNullable(newTable.getTable().getColumns())
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(ColumnValObj::getName, Function.identity()));

        // 删除不存在的字段
        for (String oldColName : oldColumns.keySet()) {
            if (!newColumns.containsKey(oldColName)) {
                changes.add(TableChange.deleteColumn(new String[]{oldColName}, true));
            }
        }

        // 添加新字段或更新字段
        for (ColumnValObj newCol : Optional.ofNullable(newTable.getTable().getColumns()).orElse(List.of())) {
            if (!oldColumns.containsKey(newCol.getName())) {
                // 添加新字段
                changes.add(TableChange.addColumn(
                        new String[]{newCol.getName()},
                        toGravitinoType(newCol.getType()),
                        newCol.getComment(),
                        true
                ));
            } else {
                // 更新字段注释
                org.apache.gravitino.rel.Column oldCol = oldColumns.get(newCol.getName());
                if (!oldCol.comment().equals(newCol.getComment())) {
                    changes.add(TableChange.updateColumnComment(new String[]{newCol.getName()}, newCol.getComment()));
                }
                // 更新字段可空性
                if (oldCol.nullable() != newCol.getNullable()) {
                    changes.add(TableChange.updateColumnNullability(new String[]{newCol.getName()}, newCol.getNullable()));
                }
            }
        }

        // 执行更新
        if (!changes.isEmpty()) {
            tableCatalog.alterTable(tableIdent, changes.toArray(new TableChange[0]));
        }
    }

    /**
     * 将字段类型转换为Gravitino类型
     */
    private org.apache.gravitino.rel.types.Type toGravitinoType(org.apache.gravitino.rel.types.Type.Name typeName) {
        return switch (typeName) {
            case STRING -> Types.StringType.get();
            case INTEGER -> Types.IntegerType.get();
            case LONG -> Types.LongType.get();
            case FLOAT -> Types.FloatType.get();
            case DOUBLE -> Types.DoubleType.get();
            case BOOLEAN -> Types.BooleanType.get();
            case DATE -> Types.DateType.get();
            case TIMESTAMP -> Types.TimestampType.withoutTimeZone();
            default -> Types.StringType.get();
        };
    }

    /**
     * 删除表
     */
    @Override
    public void delete(String id) {
        MetadataTable existingTable = metadataTableRepository.findById(id);
        Assert.notNull(existingTable, new SilentException("表不存在"));
        // 在Gravitino中删除表
        dropTableInGravitino(existingTable);
        // 从数据库删除
        metadataTableRepository.deleteById(id);
    }

    /**
     * 在Gravitino中删除表
     */
    private void dropTableInGravitino(MetadataTable table) {
        Catalog catalog = gravitinoClient.loadCatalog("iceberg");
        TableCatalog tableCatalog = catalog.asTableCatalog();
        tableCatalog.dropTable(NameIdentifier.of(table.getTable().getSchema(), table.getTable().getName()));
    }

    /**
     * 获取表列表
     */
    @Override
    public Page<MetadataTableBO> page(MetadataTablePageQuery query) {
        Page<MetadataTable> page = metadataTableRepository.page(query);
        List<MetadataTableBO> data = Optional.ofNullable(page.getData()).orElse(List.of()).stream().map(MetadataTableAppConvert.INSTANCE::toMetadataTableBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }
}