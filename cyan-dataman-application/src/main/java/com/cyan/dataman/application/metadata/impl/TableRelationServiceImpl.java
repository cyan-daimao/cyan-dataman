package com.cyan.dataman.application.metadata.impl;

import com.cyan.dataman.adapter.metadata.http.dto.TableRelationDTO;
import com.cyan.dataman.application.metadata.TableRelationService;
import com.cyan.dataman.application.metadata.cmd.CreateRelationCmd;
import com.cyan.dataman.domain.metadata.repository.TableRelationRepository;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableRelationDO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 表关系服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class TableRelationServiceImpl implements TableRelationService {

    private final TableRelationRepository tableRelationRepository;

    public TableRelationServiceImpl(TableRelationRepository tableRelationRepository) {
        this.tableRelationRepository = tableRelationRepository;
    }

    /**
     * 获取表的所有关联关系（出向+入向）
     */
    @Override
    public Map<String, List<TableRelationDTO>> getTableRelations(String catalog, String schema, String table) {
        List<MetadataTableRelationDO> outgoing = tableRelationRepository.listBySource(catalog, schema, table);
        List<MetadataTableRelationDO> incoming = tableRelationRepository.listByTarget(catalog, schema, table);

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
        MetadataTableRelationDO relation = new MetadataTableRelationDO();
        relation.setSourceCatalog(cmd.getSourceCatalog());
        relation.setSourceSchema(cmd.getSourceSchema());
        relation.setSourceTable(cmd.getSourceTable());
        relation.setSourceColumn(cmd.getSourceColumn());
        relation.setTargetCatalog(cmd.getTargetCatalog());
        relation.setTargetSchema(cmd.getTargetSchema());
        relation.setTargetTable(cmd.getTargetTable());
        relation.setTargetColumn(cmd.getTargetColumn());
        relation.setJoinType(cmd.getJoinType());
        relation.setDescription(cmd.getDescription());
        relation.setCreatedBy(createdBy);
        relation.setCreatedAt(LocalDateTime.now());
        relation.setUpdatedAt(LocalDateTime.now());

        tableRelationRepository.save(relation);
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
     * 如果找不到，再查找从维度表到事实表的入向关系（并反转方向返回）。
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
            List<MetadataTableRelationDO> outgoing = tableRelationRepository.listBySource(factCatalog, factSchema, factTable);
            Optional<MetadataTableRelationDO> direct = Optional.ofNullable(outgoing).orElse(List.of()).stream()
                    .filter(r -> dimCatalog.equals(r.getTargetCatalog())
                            && dimSchema.equals(r.getTargetSchema())
                            && dimTableName.equals(r.getTargetTable()))
                    .findFirst();

            if (direct.isPresent()) {
                result.add(toDTO(direct.get()));
                continue;
            }

            // 2. 查找维度表 -> 事实表的出向关系（即事实表的入向关系），反转方向返回
            List<MetadataTableRelationDO> incoming = tableRelationRepository.listBySource(dimCatalog, dimSchema, dimTableName);
            Optional<MetadataTableRelationDO> reverse = Optional.ofNullable(incoming).orElse(List.of()).stream()
                    .filter(r -> factCatalog.equals(r.getTargetCatalog())
                            && factSchema.equals(r.getTargetSchema())
                            && factTable.equals(r.getTargetTable()))
                    .findFirst();

            if (reverse.isPresent()) {
                MetadataTableRelationDO r = reverse.get();
                // 反转方向：把维度表作为 source，事实表作为 target
                TableRelationDTO dto = new TableRelationDTO();
                dto.setId(r.getId() != null ? r.getId().toString() : null);
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
                result.add(dto);
            }
        }

        return result;
    }

    /**
     * DO 转 DTO
     */
    private TableRelationDTO toDTO(MetadataTableRelationDO relation) {
        TableRelationDTO dto = new TableRelationDTO();
        dto.setId(relation.getId() != null ? relation.getId().toString() : null);
        dto.setSourceCatalog(relation.getSourceCatalog());
        dto.setSourceSchema(relation.getSourceSchema());
        dto.setSourceTable(relation.getSourceTable());
        dto.setSourceColumn(relation.getSourceColumn());
        dto.setTargetCatalog(relation.getTargetCatalog());
        dto.setTargetSchema(relation.getTargetSchema());
        dto.setTargetTable(relation.getTargetTable());
        dto.setTargetColumn(relation.getTargetColumn());
        dto.setJoinType(relation.getJoinType());
        dto.setDescription(relation.getDescription());
        dto.setCreatedBy(relation.getCreatedBy());
        dto.setCreatedAt(relation.getCreatedAt());
        dto.setUpdatedAt(relation.getUpdatedAt());
        return dto;
    }
}
