package com.cyan.dataman.infra.persistence.metadata.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.util.CollUtils;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.infra.persistence.metadata.convert.MetadataTableInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.dos.MetadataTableDO;
import com.cyan.dataman.infra.persistence.metadata.mappers.MetadataTableMapper;
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

    public MetadataTableRepositoryImpl(MetadataTableMapper metadataTableMapper) {
        this.metadataTableMapper = metadataTableMapper;
    }

    /**
     * 获取表列表
     */
    @Override
    public Page<MetadataTable> page(MetadataTablePageQuery query) {
        LambdaQueryWrapper<MetadataTableDO> queryWrapper = new LambdaQueryWrapper<MetadataTableDO>()
                .eq(StrUtils.isNotBlank(query.getSubjectCode()), MetadataTableDO::getSubjectCode, query.getSubjectCode())
                .eq(StrUtils.isNotBlank(query.getOwner()), MetadataTableDO::getOwner, query.getOwner())
                .and(q ->
                        q.like(StrUtils.isNotBlank(query.getName()), MetadataTableDO::getName, query.getName())
                                .or()
                                .like(StrUtils.isNotBlank(query.getComment()), MetadataTableDO::getComment, query.getComment())
                );
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
        return null;
    }

    /**
     * 获取表
     */
    @Override
    public MetadataTable findById(String id) {
        MetadataTableDO metadataTableDO = metadataTableMapper.selectById(id);
        return MetadataTableInfraConvert.INSTANCE.toMetadataTable(metadataTableDO);
    }
}
