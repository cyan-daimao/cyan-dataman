package com.cyan.dataman.application.ds.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.ds.DsConfigService;
import com.cyan.dataman.application.ds.bo.DsConfigBO;
import com.cyan.dataman.application.ds.cmd.DatabaseCreateCmd;
import com.cyan.dataman.application.ds.cmd.DsConfigCmd;
import com.cyan.dataman.application.ds.cmd.TableSchemaCmd;
import com.cyan.dataman.application.ds.convert.DsConfigAppConvert;
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.query.DsConfigFindQuery;
import com.cyan.dataman.domain.ds.query.DsConfigListQuery;
import com.cyan.dataman.domain.ds.repository.DsConfigRepository;
import com.cyan.dataman.domain.ds.valobj.ColumnValObj;
import com.cyan.dataman.domain.ds.valobj.DatabaseValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.infra.util.DsJdbcUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据源配置服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class DsConfigServiceImpl implements DsConfigService {

    private final DsConfigRepository dsConfigRepository;
    private final DsJdbcUtil dsJdbcUtil;

    public DsConfigServiceImpl(DsConfigRepository dsConfigRepository, DsJdbcUtil dsJdbcUtil) {
        this.dsConfigRepository = dsConfigRepository;
        this.dsJdbcUtil = dsJdbcUtil;
    }

    @Override
    @Transactional
    public DsConfigBO create(DsConfigCmd cmd) {
        // 检查名称是否已存在
        DsConfig existing = dsConfigRepository.find(new DsConfigFindQuery().setName(cmd.getName()));
        Assert.isNull(existing, new SilentException("数据源名称已存在"));

        DsConfig dsConfig = DsConfigAppConvert.INSTANCE.toDsConfig(cmd);
        dsConfig = dsConfig.save(dsConfigRepository);
        return DsConfigAppConvert.INSTANCE.toDsConfigBO(dsConfig);
    }

    @Override
    public List<DsConfigBO> list(DsConfigListQuery query) {
        List<DsConfig> dsConfigs = dsConfigRepository.list(query);
        return dsConfigs.stream()
                .map(DsConfigAppConvert.INSTANCE::toDsConfigBO)
                .toList();
    }

    @Override
    public DsConfigBO findById(String id) {
        DsConfig dsConfig = dsConfigRepository.findById(id);
        return DsConfigAppConvert.INSTANCE.toDsConfigBO(dsConfig);
    }

    @Override
    @Transactional
    public DsConfigBO update(String id, DsConfigCmd cmd) {
        DsConfig existing = dsConfigRepository.findById(id);
        Assert.notNull(existing, new SilentException("数据源不存在"));

        // 检查名称是否重复
        if (!existing.getName().equals(cmd.getName())) {
            DsConfig nameCheck = dsConfigRepository.find(new DsConfigFindQuery().setName(cmd.getName()));
            Assert.isNull(nameCheck, new SilentException("数据源名称已存在"));
        }

        DsConfig dsConfig = DsConfigAppConvert.INSTANCE.toDsConfig(cmd);
        dsConfig.setId(id);
        dsConfig.setCreateBy(existing.getCreateBy());
        dsConfig = dsConfig.update(dsConfigRepository);
        return DsConfigAppConvert.INSTANCE.toDsConfigBO(dsConfig);
    }

    @Override
    @Transactional
    public void delete(String id) {
        DsConfig dsConfig = dsConfigRepository.findById(id);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        dsConfig.delete(dsConfigRepository);
    }

    @Override
    public void testConnection(String id) {
        DsConfig dsConfig = dsConfigRepository.findById(id);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        dsJdbcUtil.testConnection(dsConfig);
    }

    @Override
    public List<DatabaseValObj> listDatabases(String dsId) {
        DsConfig dsConfig = dsConfigRepository.findById(dsId);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        return dsJdbcUtil.listDatabases(dsConfig);
    }

    @Override
    public void createDatabase(String dsId, DatabaseCreateCmd cmd) {
        DsConfig dsConfig = dsConfigRepository.findById(dsId);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        dsJdbcUtil.createDatabase(dsConfig, cmd.getName(), cmd.getCharset(), cmd.getCollation());
    }

    @Override
    public List<String> listTables(String dsId, String dbName) {
        DsConfig dsConfig = dsConfigRepository.findById(dsId);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        return dsJdbcUtil.listTables(dsConfig, dbName);
    }

    @Override
    public TableSchemaValObj getTableSchema(String dsId, String dbName, String tableName) {
        DsConfig dsConfig = dsConfigRepository.findById(dsId);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        return dsJdbcUtil.getTableSchema(dsConfig, dbName, tableName);
    }

    @Override
    @Transactional
    public void createTable(String dsId, String dbName, TableSchemaCmd cmd) {
        DsConfig dsConfig = dsConfigRepository.findById(dsId);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        
        TableSchemaValObj tableSchema = new TableSchemaValObj()
                .setTableName(cmd.getTableName())
                .setTableComment(cmd.getTableComment())
                .setColumns(addDefaultColumns(dsConfig, cmd.getColumns()))
                .setIndexes(cmd.getIndexes());
        
        dsJdbcUtil.createTable(dsConfig, dbName, tableSchema);
    }

    @Override
    @Transactional
    public void updateTable(String dsId, String dbName, String tableName, TableSchemaCmd cmd) {
        DsConfig dsConfig = dsConfigRepository.findById(dsId);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        
        TableSchemaValObj tableSchema = new TableSchemaValObj()
                .setTableName(cmd.getTableName())
                .setTableComment(cmd.getTableComment())
                .setColumns(addDefaultColumns(dsConfig, cmd.getColumns()))
                .setIndexes(cmd.getIndexes());
        
        dsJdbcUtil.updateTable(dsConfig, dbName, tableName, tableSchema);
    }

    @Override
    @Transactional
    public void dropTable(String dsId, String dbName, String tableName) {
        DsConfig dsConfig = dsConfigRepository.findById(dsId);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        dsJdbcUtil.dropTable(dsConfig, dbName, tableName);
    }

    /**
     * 添加默认字段
     */
    private List<ColumnValObj> addDefaultColumns(DsConfig dsConfig, List<ColumnValObj> columns) {
        List<ColumnValObj> result = new java.util.ArrayList<>(columns);
        
        // 检查是否存在默认字段，不存在则添加
        boolean hasCreatedAt = columns.stream().anyMatch(c -> "created_at".equalsIgnoreCase(c.getName()));
        boolean hasUpdatedAt = columns.stream().anyMatch(c -> "updated_at".equalsIgnoreCase(c.getName()));
        boolean hasDeletedAt = columns.stream().anyMatch(c -> "deleted_at".equalsIgnoreCase(c.getName()));
        
        if (!hasCreatedAt) {
            result.add(dsJdbcUtil.createColumnValObj(dsConfig.getDatasourceType())
                    .setName("created_at")
                    .setType("datetime")
                    .setComment("创建时间")
                    .setNullable(true));
        }
        if (!hasUpdatedAt) {
            result.add(dsJdbcUtil.createColumnValObj(dsConfig.getDatasourceType())
                    .setName("updated_at")
                    .setType("datetime")
                    .setComment("更新时间")
                    .setNullable(true));
        }
        if (!hasDeletedAt) {
            result.add(dsJdbcUtil.createColumnValObj(dsConfig.getDatasourceType())
                    .setName("deleted_at")
                    .setType("datetime")
                    .setComment("删除时间")
                    .setNullable(true));
        }
        
        return result;
    }
}
