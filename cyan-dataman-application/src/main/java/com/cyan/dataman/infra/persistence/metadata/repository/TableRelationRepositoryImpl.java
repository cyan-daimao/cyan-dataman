package com.cyan.dataman.infra.persistence.metadata.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.metadata.repository.TableRelationRepository;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableRelationDO;
import com.cyan.dataman.infra.persistence.metadata.mappers.MetadataTableRelationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 表关系仓库实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class TableRelationRepositoryImpl implements TableRelationRepository {

    private final MetadataTableRelationMapper metadataTableRelationMapper;

    public TableRelationRepositoryImpl(MetadataTableRelationMapper metadataTableRelationMapper) {
        this.metadataTableRelationMapper = metadataTableRelationMapper;
    }

    /**
     * 保存表关系
     */
    @Override
    public MetadataTableRelationDO save(MetadataTableRelationDO relation) {
        metadataTableRelationMapper.insert(relation);
        return relation;
    }

    /**
     * 根据ID删除表关系
     */
    @Override
    public void deleteById(Long id) {
        metadataTableRelationMapper.deleteById(id);
    }

    /**
     * 根据源表查询关系列表
     */
    @Override
    public List<MetadataTableRelationDO> listBySource(String catalog, String schema, String table) {
        LambdaQueryWrapper<MetadataTableRelationDO> wrapper = new LambdaQueryWrapper<MetadataTableRelationDO>()
                .eq(MetadataTableRelationDO::getSourceCatalog, catalog)
                .eq(MetadataTableRelationDO::getSourceSchema, schema)
                .eq(MetadataTableRelationDO::getSourceTable, table);
        return metadataTableRelationMapper.selectList(wrapper);
    }

    /**
     * 根据目标表查询关系列表
     */
    @Override
    public List<MetadataTableRelationDO> listByTarget(String catalog, String schema, String table) {
        LambdaQueryWrapper<MetadataTableRelationDO> wrapper = new LambdaQueryWrapper<MetadataTableRelationDO>()
                .eq(MetadataTableRelationDO::getTargetCatalog, catalog)
                .eq(MetadataTableRelationDO::getTargetSchema, schema)
                .eq(MetadataTableRelationDO::getTargetTable, table);
        return metadataTableRelationMapper.selectList(wrapper);
    }
}
