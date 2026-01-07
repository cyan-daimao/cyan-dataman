package com.cyan.dataman.infra.datasource.repository;

import com.cyan.arch.common.api.SilentException;
import com.cyan.arch.common.util.Convert;
import com.cyan.dataman.domain.datasource.DatasourceSchema;
import com.cyan.dataman.domain.datasource.DatasourceTable;
import com.cyan.dataman.domain.datasource.query.DatasourceTableQuery;
import com.cyan.dataman.domain.datasource.repository.DatasourceRepository;
import com.cyan.dataman.enums.StorageType;
import com.cyan.dataman.infra.util.JDBCUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * mysql仓储服务
 *
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
    public List<DatasourceSchema> listDB() {
        String sql = "SHOW DATABASES";
        try {
            List<Map<String, Object>> maps = JDBCUtil.queryForList(sql);
            return Optional.of(maps).orElse(List.of()).stream().map(map -> {
                String db = "";
                for (Object value : map.values()) {
                    db = Convert.toStr(value);
                }
                return new DatasourceSchema().setDb(db);
            }).filter(db -> db.getDb().contains("cyan_")).toList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * 获取数据源-表列表
     */
    @Override
    public List<DatasourceTable> listTable(DatasourceTableQuery query) {
        if (StringUtils.isBlank(query.getDb())){
            throw new SilentException("db不能为空");
        }
        String sql = """
                SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'
                """;
        try {
            List<Map<String, Object>> maps = JDBCUtil.queryForList(sql, query.getDb());
            return Optional.of(maps).orElse(List.of()).stream().map(map ->
                    new DatasourceTable().setDb(query.getDb()).setName(Convert.toStr(map.get("TABLE_NAME")))
            ).toList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
