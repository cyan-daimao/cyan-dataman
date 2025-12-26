package com.cyan.dataman.infra.datasource.repository;

import com.cyan.dataman.domain.datasource.DatasourceTable;
import com.cyan.dataman.domain.datasource.repository.DatasourceRepository;
import com.cyan.dataman.enums.StorageType;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * mysql仓储服务
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class MySQLRepositoryImpl implements DatasourceRepository {
    /**
     * 获取数据源表列表
     *
     * @return 数据源表列表
     */
    @Override
    public List<DatasourceTable> list() {
        return List.of();
    }

    /**
     * 获取数据源类型
     *
     * @return 数据源类型
     */
    @Override
    public StorageType getStorageType() {
        return StorageType.MYSQL;
    }
}
