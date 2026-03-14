package com.cyan.dataman.infra.persistence.metadata.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.util.CollUtils;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.infra.persistence.metadata.convert.MetadataColumnInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.convert.MetadataTableInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataColumnDO;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableDO;
import com.cyan.dataman.infra.persistence.metadata.mappers.MetadataColumnMapper;
import com.cyan.dataman.infra.persistence.metadata.mappers.MetadataTableMapper;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 元数据表仓库实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class MetadataTableRepositoryImpl implements MetadataTableRepository {
    private final MetadataTableMapper metadataTableMapper;
    private final MetadataColumnMapper metadataColumnMapper;

    public MetadataTableRepositoryImpl(MetadataTableMapper metadataTableMapper, MetadataColumnMapper metadataColumnMapper) {
        this.metadataTableMapper = metadataTableMapper;
        this.metadataColumnMapper = metadataColumnMapper;
    }

    /**
     * 获取表列表
     */
    @Override
    public Page<MetadataTable> page(MetadataTablePageQuery query) {
        LambdaQueryWrapper<MetadataTableDO> queryWrapper = new LambdaQueryWrapper<MetadataTableDO>()
                .eq(StrUtils.isNotBlank(query.getSubjectCode()), MetadataTableDO::getSubjectCode, query.getSubjectCode())
                .eq(StrUtils.isNotBlank(query.getOwner()), MetadataTableDO::getOwner, query.getOwner());
        if (StringUtils.isNotBlank(query.getName()) || StringUtils.isNotBlank(query.getComment())) {
            queryWrapper.and(q ->
                    q.like(StrUtils.isNotBlank(query.getName()), MetadataTableDO::getTbl, query.getName())
                            .or()
                            .like(StrUtils.isNotBlank(query.getComment()), MetadataTableDO::getComment, query.getComment())
            );
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MetadataTableDO> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getCurrent(), query.getSize());
        page = metadataTableMapper.selectPage(page, queryWrapper);
        List<MetadataTable> data = Optional.ofNullable(page.getRecords()).orElse(List.of()).stream().map(MetadataTableInfraConvert.INSTANCE::toMetadataTable).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 获取表列表
     */
    @Override
    public List<MetadataTable> list(MetadataTableListQuery query) {
        LambdaQueryWrapper<MetadataTableDO> queryWrapper = new LambdaQueryWrapper<MetadataTableDO>()
                .in(CollUtils.isNotEmpty(query.getIds()), MetadataTableDO::getId, query.getIds());
        List<MetadataTableDO> metadataTables = metadataTableMapper.selectList(queryWrapper);
        return Optional.ofNullable(metadataTables).orElse(List.of()).stream().map(MetadataTableInfraConvert.INSTANCE::toMetadataTable).toList();
    }

    /**
     * 保存表
     */
    @Override
    public MetadataTable save(MetadataTable table) {
        MetadataTableDO metadataTableDO = MetadataTableInfraConvert.INSTANCE.toMetadataTableDO(table);
        metadataTableDO.setDataCatalog("iceberg");
        metadataTableMapper.insert(metadataTableDO);
        // 保存字段信息
        saveColumns(table, metadataTableDO);
        return findById(metadataTableDO.getId() + "");
    }

    /**
     * 保存字段信息
     */
    private void saveColumns(MetadataTable table, MetadataTableDO metadataTableDO) {
        if (CollUtils.isNotEmpty(table.getTable().getColumns())) {
            List<MetadataColumnDO> columnDOs = MetadataColumnInfraConvert.INSTANCE.toMetadataColumnDOList(table.getTable().getColumns());
            columnDOs.forEach(col -> {
                col.setDataCatalog(metadataTableDO.getDataCatalog());
                col.setDataSchema(metadataTableDO.getDataSchema());
                col.setTbl(metadataTableDO.getTbl());
                metadataColumnMapper.insert(col);
            });
        }
    }

    /**
     * 获取表
     */
    @Override
    public MetadataTable findById(String id) {
        MetadataTableDO metadataTableDO = metadataTableMapper.selectById(id);
        if (metadataTableDO == null){
            return null;
        }
        LambdaQueryWrapper<MetadataColumnDO> queryWrapper = new LambdaQueryWrapper<MetadataColumnDO>()
                .eq(MetadataColumnDO::getTbl, metadataTableDO.getTbl());
        List<MetadataColumnDO> metadataColumnDOS = metadataColumnMapper.selectList(queryWrapper);
        List<ColumnValObj> cols = MetadataTableInfraConvert.INSTANCE.toMetadataColumns(metadataColumnDOS);
        MetadataTable metadataTable = MetadataTableInfraConvert.INSTANCE.toMetadataTable(metadataTableDO);
        metadataTable.getTable().setColumns(cols);
        return metadataTable;
    }

    /**
     * 删除表
     */
    @Override
    public void deleteById(String id) {
        MetadataTable table = findById(id);
        // 删除表
        metadataTableMapper.deleteById(id);
        // 删除字段
        metadataColumnMapper.delete(new LambdaQueryWrapper<MetadataColumnDO>().eq(MetadataColumnDO::getTbl, table.getName()));
    }

    /**
     * 更新表
     */
    @Override
    public MetadataTable updateById(MetadataTable table) {
        MetadataTableDO metadataTableDO = MetadataTableInfraConvert.INSTANCE.toMetadataTableDO(table);
        metadataTableMapper.updateById(metadataTableDO);
        // 删除旧字段并保存新字段
        updateColumns(table, metadataTableDO);
        return findById(metadataTableDO.getId() + "");
    }

    /**
     * 获取表
     */
    @Override
    public MetadataTable findOne(MetadataTableOneQuery query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<MetadataTableDO> queryWrapper = new LambdaQueryWrapper<MetadataTableDO>()
                .eq(StrUtils.isNotBlank(query.getName()), MetadataTableDO::getTbl, query.getName())
                .last("limit 1");
        MetadataTableDO metadataTableDO = metadataTableMapper.selectOne(queryWrapper);
        return MetadataTableInfraConvert.INSTANCE.toMetadataTable(metadataTableDO);
    }

    /**
     * 更新字段信息
     */
    private void updateColumns(MetadataTable table, MetadataTableDO metadataTableDO) {
        // 删除旧字段
        metadataColumnMapper.delete(new LambdaQueryWrapper<MetadataColumnDO>()
                .eq(MetadataColumnDO::getDataCatalog, metadataTableDO.getDataCatalog())
                .eq(MetadataColumnDO::getDataSchema, metadataTableDO.getDataSchema())
                .eq(MetadataColumnDO::getTbl, metadataTableDO.getTbl()));
        // 保存新字段
        saveColumns(table, metadataTableDO);
    }
}