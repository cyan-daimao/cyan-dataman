package com.cyan.dataman.application.metadata.impl;

import com.cyan.arch.common.api.Page;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.convert.MetadataTableAppConvert;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 元数据服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class MetadataTableServiceImpl implements MetadataTableService {
    private final MetadataTableRepository metadataTableRepository;

    public MetadataTableServiceImpl(MetadataTableRepository metadataTableRepository) {
        this.metadataTableRepository = metadataTableRepository;
    }

    /**
     * 获取表列表
     *
     */
    @Override
    public List<MetadataTableBO> list(MetadataTableListQuery query) {
        List<MetadataTable> tables = metadataTableRepository.list(query);
        return Optional.ofNullable(tables).orElse(List.of()).stream().map(MetadataTableAppConvert.INSTANCE::toMetadataTableBO).toList();
    }

    /**
     * 获取表
     *
     */
    @Override
    public MetadataTableBO findById(String id) {
        MetadataTable metadataTable = metadataTableRepository.findById(id);
        return MetadataTableAppConvert.INSTANCE.toMetadataTableBO(metadataTable);
    }

    /**
     * 获取表列表
     *
     */
    @Override
    public Page<MetadataTableBO> page(MetadataTablePageQuery query) {
        Page<MetadataTable> page = metadataTableRepository.page(query);
        List<MetadataTableBO> data = Optional.ofNullable(page.getData()).orElse(List.of()).stream().map(MetadataTableAppConvert.INSTANCE::toMetadataTableBO).toList();
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }
}
