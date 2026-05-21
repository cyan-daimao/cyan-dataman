package com.cyan.dataman.application.metadata.impl;

import com.cyan.dataman.client.table.dto.TableRelationDTO;
import com.cyan.dataman.application.metadata.TableRelationService;
import com.cyan.dataman.application.metadata.cmd.CreateRelationCmd;
import com.cyan.dataman.application.metadata.convert.TableRelationAppConvert;
import com.cyan.dataman.domain.metadata.TableRelation;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.domain.metadata.repository.TableRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 表关系服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class TableRelationServiceImpl implements TableRelationService {

    private final TableRelationRepository tableRelationRepository;
    private final MetadataTableRepository metadataTableRepository;

    public TableRelationServiceImpl(TableRelationRepository tableRelationRepository,
                                    MetadataTableRepository metadataTableRepository) {
        this.tableRelationRepository = tableRelationRepository;
        this.metadataTableRepository = metadataTableRepository;
    }

    /**
     * 获取表的所有关联关系（出向+入向）
     */
    @Override
    public Map<String, List<TableRelationDTO>> getTableRelations(String catalog, String schema, String table) {
        List<TableRelation> outgoing = tableRelationRepository.listBySource(catalog, schema, table);
        List<TableRelation> incoming = tableRelationRepository.listByTarget(catalog, schema, table);

        Map<String, List<TableRelationDTO>> result = new HashMap<>();
        result.put("outgoing", Optional.ofNullable(outgoing).orElse(List.of()).stream()
                .map(this::toDTO)
                .toList());
        result.put("incoming", Optional.ofNullable(incoming).orElse(List.of()).stream()
                .map(this::toDTO)
                .toList());
        return result;
    }

    /**
     * 创建关联关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TableRelationDTO createRelation(CreateRelationCmd cmd, String createdBy) {
        TableRelation relation = new TableRelation()
                .setSourceCatalog(cmd.getSourceCatalog())
                .setSourceSchema(cmd.getSourceSchema())
                .setSourceTable(cmd.getSourceTable())
                .setSourceColumn(cmd.getSourceColumn())
                .setTargetCatalog(cmd.getTargetCatalog())
                .setTargetSchema(cmd.getTargetSchema())
                .setTargetTable(cmd.getTargetTable())
                .setTargetColumn(cmd.getTargetColumn())
                .setJoinType(cmd.getJoinType())
                .setDescription(cmd.getDescription())
                .setCreatedBy(createdBy)
                .setUpdatedBy(createdBy)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());

        relation = relation.save(tableRelationRepository);
        return toDTO(relation);
    }

    /**
     * 删除关联关系
     */
    @Override
    public void deleteRelation(Long id) {
        tableRelationRepository.deleteById(id);
    }

    /**
     * 批量查询 JOIN 路径
     *
     * <p>对于每个维度表，先查找从事实表到维度表的直接出向关系；
     * 如果找不到，再查找从维度表到事实表的出向关系（并反转方向返回）。
     */
    @Override
    public List<TableRelationDTO> findJoinPaths(String factCatalog, String factSchema, String factTable,
                                                  List<String[]> dimensionTables) {
        if (dimensionTables == null || dimensionTables.isEmpty()) {
            return List.of();
        }

        List<TableRelationDTO> result = new ArrayList<>();

        for (String[] dimTable : dimensionTables) {
            if (dimTable == null || dimTable.length < 3) {
                continue;
            }
            String dimCatalog = dimTable[0];
            String dimSchema = dimTable[1];
            String dimTableName = dimTable[2];

            // 如果维度表和事实表相同，跳过
            if (factCatalog.equals(dimCatalog) && factSchema.equals(dimSchema) && factTable.equals(dimTableName)) {
                continue;
            }

            // 1. 查找事实表 -> 维度表的出向关系
            List<TableRelation> outgoing = tableRelationRepository.listBySource(factCatalog, factSchema, factTable);
            Optional<TableRelation> direct = Optional.ofNullable(outgoing).orElse(List.of()).stream()
                    .filter(r -> dimCatalog.equals(r.getTargetCatalog())
                            && dimSchema.equals(r.getTargetSchema())
                            && dimTableName.equals(r.getTargetTable()))
                    .findFirst();

            if (direct.isPresent()) {
                result.add(toDTO(direct.get()));
                continue;
            }

            // 2. 查找维度表 -> 事实表的出向关系（即事实表的入向关系），反转方向返回
            List<TableRelation> incoming = tableRelationRepository.listBySource(dimCatalog, dimSchema, dimTableName);
            Optional<TableRelation> reverse = Optional.ofNullable(incoming).orElse(List.of()).stream()
                    .filter(r -> factCatalog.equals(r.getTargetCatalog())
                            && factSchema.equals(r.getTargetSchema())
                            && factTable.equals(r.getTargetTable()))
                    .findFirst();

            if (reverse.isPresent()) {
                TableRelation r = reverse.get();
                // 反转方向：把维度表作为 source，事实表作为 target
                TableRelationDTO dto = TableRelationAppConvert.INSTANCE.toTableRelationDTO(r);
                dto.setSourceCatalog(r.getTargetCatalog());
                dto.setSourceSchema(r.getTargetSchema());
                dto.setSourceTable(r.getTargetTable());
                dto.setSourceColumn(r.getTargetColumn());
                dto.setTargetCatalog(r.getSourceCatalog());
                dto.setTargetSchema(r.getSourceSchema());
                dto.setTargetTable(r.getSourceTable());
                dto.setTargetColumn(r.getSourceColumn());
                dto.setJoinType(r.getJoinType());
                dto.setDescription(r.getDescription());
                dto.setCreatedBy(r.getCreatedBy());
                dto.setCreatedAt(r.getCreatedAt());
                dto.setUpdatedAt(r.getUpdatedAt());
                dto.setSourceTableComment(getTableComment(r.getTargetCatalog(), r.getTargetSchema(), r.getTargetTable()));
                dto.setTargetTableComment(getTableComment(r.getSourceCatalog(), r.getSourceSchema(), r.getSourceTable()));
                result.add(dto);
            }
        }

        return result;
    }

    /**
     * Domain 转 DTO
     */
    private TableRelationDTO toDTO(TableRelation relation) {
        TableRelationDTO dto = TableRelationAppConvert.INSTANCE.toTableRelationDTO(relation);
        dto.setSourceTableComment(getTableComment(relation.getSourceCatalog(), relation.getSourceSchema(), relation.getSourceTable()));
        dto.setTargetTableComment(getTableComment(relation.getTargetCatalog(), relation.getTargetSchema(), relation.getTargetTable()));
        return dto;
    }

    /**
     * 根据 catalog + schema + table 查询表注释
     */
    private String getTableComment(String catalog, String schema, String table) {
        return metadataTableRepository.findCommentByCatalogSchemaTable(catalog, schema, table);
    }
}
