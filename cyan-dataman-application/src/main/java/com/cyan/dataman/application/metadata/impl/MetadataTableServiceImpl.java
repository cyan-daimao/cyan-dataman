package com.cyan.dataman.application.metadata.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.adapter.metadata.http.dto.SubjectTableTreeDTO;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.application.metadata.convert.MetadataTableAppConvert;
import com.cyan.dataman.domain.metadata.MetadataSubject;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataSubjectListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataSubjectRepository;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.enums.ColumnDataType;
import com.cyan.dataman.enums.DatasourceType;
import io.micrometer.common.util.StringUtils;
import org.apache.gravitino.Catalog;
import org.apache.gravitino.NameIdentifier;
import org.apache.gravitino.client.GravitinoClient;
import org.apache.gravitino.rel.Table;
import org.apache.gravitino.rel.TableCatalog;
import org.apache.gravitino.rel.TableChange;
import org.apache.gravitino.rel.types.Type;
import org.apache.gravitino.rel.types.Types;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final MetadataSubjectRepository metadataSubjectRepository;
    private final GravitinoClient gravitinoClient;

    public MetadataTableServiceImpl(MetadataTableRepository metadataTableRepository,
                                    MetadataSubjectRepository metadataSubjectRepository,
                                    GravitinoClient gravitinoClient) {
        this.metadataTableRepository = metadataTableRepository;
        this.metadataSubjectRepository = metadataSubjectRepository;
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
    @Transactional(rollbackFor = Exception.class)
    public MetadataTableBO save(MetadataTableCmd cmd) {
        MetadataTableBO one = findOne(new MetadataTableOneQuery().setName(cmd.getName()));
        Assert.isTrue(one == null, new SilentException("表已存在"));
        MetadataTable metadataTable = MetadataTableAppConvert.INSTANCE.toMetadataTable(cmd);
        metadataTable.setDatasourceType(DatasourceType.ICEBERG);
        metadataTable.setLayerCode(cmd.getLayerCode().getCode().toLowerCase());
        metadataTable.getTable().setSchema(cmd.getLayerCode().getCode().toLowerCase());
        // 保存到数据库
        metadataTable = metadataTable.save(metadataTableRepository);
        // 在Gravitino中创建表
        createTableInGravitino(metadataTable);
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
                //默认保留10个metadata.json文件
                Map.of(
                        "write.metadata.delete-after-commit.enabled", "true",
                        "write.metadata.previous-versions-max", "10"
                )
        );
    }

    /**
     * 转换为Gravitino字段
     */
    private org.apache.gravitino.rel.Column toGravitinoColumn(ColumnValObj columnValObj) {
        return org.apache.gravitino.rel.Column.of(
                columnValObj.getName(),
                toGravitinoType(columnValObj.getType(), columnValObj.getPrecision(), columnValObj.getScale()),
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
    @Transactional(rollbackFor = Exception.class)
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
        // 更新数据库
        metadataTable = metadataTable.update(metadataTableRepository);
        // 在Gravitino中更新表
        updateTableInGravitino(existingTable, metadataTable);
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
                        toGravitinoType(newCol.getType(), newCol.getPrecision(), newCol.getScale()),
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
    private Type toGravitinoType(ColumnDataType columnDataType, Integer precision, Integer scale) {
        return switch (columnDataType) {
            case BOOLEAN -> Types.BooleanType.get();
            case INTEGER -> Types.IntegerType.get();
            case LONG -> Types.LongType.get();
            case FLOAT -> Types.FloatType.get();
            case DOUBLE -> Types.DoubleType.get();
            case DECIMAL -> {
                int p = precision != null && precision > 0 ? precision : 10;
                int s = scale != null && scale >= 0 ? scale : 0;
                yield Types.DecimalType.of(p, s);
            }
            case STRING -> Types.StringType.get();
            case DATE -> Types.DateType.get();
            case TIMESTAMP -> Types.TimestampType.withoutTimeZone();
            case TIMESTAMP_TZ -> Types.TimestampType.withTimeZone();
            case TIME -> Types.TimeType.get();
            case BINARY -> Types.BinaryType.get();
            case UUID -> Types.UUIDType.get();
        };
    }

    /**
     * 删除表
     */
    @Override
    public void delete(String id) {
        MetadataTable existingTable = metadataTableRepository.findById(id);
        Assert.notNull(existingTable, new SilentException("表不存在"));
        // 从数据库删除
        metadataTableRepository.deleteById(id);
        // 在Gravitino中删除表
        dropTableInGravitino(existingTable);
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

    /**
     * 获取主题-表树形结构
     *
     * @param content 搜索内容（表名或表描述）
     * @return 树形结构
     */
    @Override
    public List<SubjectTableTreeDTO> getSubjectTableTree(String content) {
        // 1. 获取所有主题
        List<MetadataSubject> allSubjects = metadataSubjectRepository.list(new MetadataSubjectListQuery());

        // 2. 获取表列表（支持搜索）
        MetadataTableListQuery tableQuery = new MetadataTableListQuery();
        if (StringUtils.isNotBlank(content)) {
            tableQuery.setContent(content);
        }
        List<MetadataTable> allTables = metadataTableRepository.list(tableQuery);

        // 3. 构建主题 Map（key: subjectCode）
        Map<String, MetadataSubject> subjectCodeMap = Optional.ofNullable(allSubjects).orElse(List.of())
                .stream()
                .collect(Collectors.toMap(MetadataSubject::getSubjectCode, Function.identity(), (a, b) -> a));

        // 4. 构建主题 ID -> 主题对象 Map
        Map<String, MetadataSubject> subjectIdMap = Optional.ofNullable(allSubjects).orElse(List.of())
                .stream()
                .collect(Collectors.toMap(MetadataSubject::getId, Function.identity(), (a, b) -> a));

        // 5. 根据搜索条件确定需要显示的主题
        Set<String> visibleSubjectCodes = new HashSet<>();
        if (StringUtils.isNotBlank(content)) {
            // 搜索模式：只显示包含匹配表的主题及其祖先
            for (MetadataTable table : Optional.ofNullable(allTables).orElse(List.of())) {
                if (table.getSubjectCode() != null) {
                    visibleSubjectCodes.add(table.getSubjectCode());
                    // 添加祖先主题
                    addAncestorSubjects(table.getSubjectCode(), subjectCodeMap, subjectIdMap, visibleSubjectCodes);
                }
            }
        } else {
            // 非搜索模式：显示所有主题
            visibleSubjectCodes.addAll(subjectCodeMap.keySet());
        }

        // 6. 过滤主题并构建树形结构
        List<MetadataSubject> filteredSubjects = Optional.ofNullable(allSubjects).orElse(List.of())
                .stream()
                .filter(s -> visibleSubjectCodes.contains(s.getSubjectCode()))
                .collect(Collectors.toList());

        // 7. 构建主题树
        return buildSubjectTree(filteredSubjects, allTables, visibleSubjectCodes);
    }

    /**
     * 添加祖先主题到可见集合
     */
    private void addAncestorSubjects(String subjectCode, Map<String, MetadataSubject> subjectCodeMap,
                                     Map<String, MetadataSubject> subjectIdMap, Set<String> visibleSubjectCodes) {
        MetadataSubject subject = subjectCodeMap.get(subjectCode);
        if (subject == null) return;

        String parentId = subject.getParentId();
        while (parentId != null && !"0".equals(parentId)) {
            MetadataSubject parent = subjectIdMap.get(parentId);
            if (parent != null) {
                visibleSubjectCodes.add(parent.getSubjectCode());
                parentId = parent.getParentId();
            } else {
                break;
            }
        }
    }

    /**
     * 构建主题树形结构
     */
    private List<SubjectTableTreeDTO> buildSubjectTree(List<MetadataSubject> subjects,
                                                       List<MetadataTable> tables,
                                                       Set<String> visibleSubjectCodes) {
        // 构建主题 ID -> 主题对象 Map
        Map<String, MetadataSubject> subjectIdMap = subjects.stream()
                .collect(Collectors.toMap(MetadataSubject::getId, Function.identity(), (a, b) -> a));

        // 按 subjectCode 分组表
        Map<String, List<MetadataTable>> tablesBySubject = Optional.ofNullable(tables).orElse(List.of())
                .stream()
                .filter(t -> t.getSubjectCode() != null && visibleSubjectCodes.contains(t.getSubjectCode()))
                .collect(Collectors.groupingBy(MetadataTable::getSubjectCode));

        // 构建树节点
        Map<String, SubjectTableTreeDTO> nodeMap = new java.util.HashMap<>();
        for (MetadataSubject subject : subjects) {
            SubjectTableTreeDTO node = new SubjectTableTreeDTO()
                    .setKey("subject-" + subject.getId())
                    .setTitle(subject.getSubjectName())
                    .setType("subject")
                    .setSubjectCode(subject.getSubjectCode())
                    .setLeaf(false);
            nodeMap.put(subject.getId(), node);
        }

        // 构建父子关系
        List<SubjectTableTreeDTO> rootNodes = new ArrayList<>();
        for (MetadataSubject subject : subjects) {
            SubjectTableTreeDTO node = nodeMap.get(subject.getId());
            String parentId = subject.getParentId();

            if (parentId == null || "0".equals(parentId)) {
                // 一级主题
                rootNodes.add(node);
            } else {
                // 子主题，添加到父节点
                SubjectTableTreeDTO parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    if (parentNode.getChildren() == null) {
                        parentNode.setChildren(new ArrayList<>());
                    }
                    parentNode.getChildren().add(node);
                }
            }
        }

        // 为每个主题节点添加表作为子节点
        for (MetadataSubject subject : subjects) {
            SubjectTableTreeDTO node = nodeMap.get(subject.getId());
            List<MetadataTable> subjectTables = tablesBySubject.get(subject.getSubjectCode());
            if (subjectTables != null && !subjectTables.isEmpty()) {
                if (node.getChildren() == null) {
                    node.setChildren(new ArrayList<>());
                }
                for (MetadataTable table : subjectTables) {
                    SubjectTableTreeDTO tableNode = new SubjectTableTreeDTO()
                            .setKey("table-" + table.getId())
                            .setTitle(table.getName() + (table.getComment() != null ? " (" + table.getComment() + ")" : ""))
                            .setType("table")
                            .setTableId(table.getId())
                            .setTableName(table.getName())
                            .setCatalog(table.getTable() != null ? table.getTable().getCatalog() : null)
                            .setSchema(table.getTable() != null ? table.getTable().getSchema() : null)
                            .setLeaf(true);
                    node.getChildren().add(tableNode);
                }
            }
            // 更新 isLeaf 状态
            node.setLeaf(node.getChildren() == null || node.getChildren().isEmpty());
        }

        return rootNodes;
    }
}