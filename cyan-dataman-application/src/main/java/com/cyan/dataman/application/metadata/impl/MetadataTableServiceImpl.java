package com.cyan.dataman.application.metadata.impl;

import com.cyan.arch.common.api.Page;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.application.metadata.cmd.ImportTableCmd;
import com.cyan.dataman.application.metadata.cmd.MetadataTableCmd;
import com.cyan.dataman.application.metadata.convert.MetadataTableAppConvert;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import org.apache.gravitino.Catalog;
import org.apache.gravitino.NameIdentifier;
import org.apache.gravitino.client.GravitinoClient;
import org.apache.gravitino.rel.Table;
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
        MetadataTable metadataTable = MetadataTableAppConvert.INSTANCE.toMetadataTable(cmd);
        metadataTable = metadataTable.save(metadataTableRepository);
        return MetadataTableAppConvert.INSTANCE.toMetadataTableBO(metadataTable);
    }

    /**
     * 更新表
     */
    @Override
    public MetadataTableBO update(String id, MetadataTableCmd cmd) {
        return null;
    }

    /**
     * 导入表
     */
    @Override
    public MetadataTableBO importTable(ImportTableCmd cmd) {
        Catalog catalog = gravitinoClient.loadCatalog(cmd.getCatalog());
        Table table = catalog.asTableCatalog().loadTable(NameIdentifier.of(cmd.getSchema(),cmd.getTable()));
        return null;
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
