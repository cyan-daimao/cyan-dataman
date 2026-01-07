package com.cyan.dataman.application.datasource.impl;

import com.cyan.dataman.application.datasource.DatasourceService;
import com.cyan.dataman.application.datasource.bo.DatasourceSchemaBO;
import com.cyan.dataman.application.datasource.bo.DatasourceTableBO;
import com.cyan.dataman.application.datasource.convert.DatasourceAppConvert;
import com.cyan.dataman.domain.datasource.DatasourceSchema;
import com.cyan.dataman.domain.datasource.DatasourceTable;
import com.cyan.dataman.domain.datasource.query.DatasourceTableQuery;
import com.cyan.dataman.domain.datasource.repository.DatasourceRepository;
import com.cyan.dataman.enums.StorageType;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据源服务
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class DatasourceServiceImpl implements DatasourceService {
    private final Map<StorageType, DatasourceRepository> repositoryMap = new EnumMap<>(StorageType.class);

    public DatasourceServiceImpl(List<DatasourceRepository> repositories) {
        for (DatasourceRepository repository : repositories) {
            repositoryMap.put(repository.getStorageType(), repository);
        }
    }

    /**
     * 获取数据源-库
     *
     */
    @Override
    public List<DatasourceSchemaBO> listDB(@Validated DatasourceTableQuery query) {
        DatasourceRepository datasourceRepository = repositoryMap.get(query.getStorageType());
        List<DatasourceSchema> list = datasourceRepository.listDB();
        return Optional.ofNullable(list).orElse(List.of()).stream().map(DatasourceAppConvert.INSTANCE::toDatasourceSchemaBO).toList();
    }

    /**
     * 获取数据源-表
     *
     */
    @Override
    public List<DatasourceTableBO> listTable(@Validated DatasourceTableQuery query) {
        DatasourceRepository datasourceRepository = repositoryMap.get(query.getStorageType());
        List<DatasourceTable> datasourceTables = datasourceRepository.listTable(query);
        return Optional.ofNullable(datasourceTables).orElse(List.of()).stream().map(DatasourceAppConvert.INSTANCE::toDatasourceTableBO).toList();
    }
}
