package com.cyan.dataman.application.ds.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.cdc.service.CdcSchemaSyncService;
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
import com.cyan.dataman.enums.DatasourceType;
import com.cyan.dataman.infra.util.DsJdbcUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 数据源配置服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class DsConfigServiceImpl implements DsConfigService {

    private final DsConfigRepository dsConfigRepository;
    private final DsJdbcUtil dsJdbcUtil;
    private final CdcSchemaSyncService cdcSchemaSyncService;

    public DsConfigServiceImpl(DsConfigRepository dsConfigRepository,
                               DsJdbcUtil dsJdbcUtil,
                               CdcSchemaSyncService cdcSchemaSyncService) {
        this.dsConfigRepository = dsConfigRepository;
        this.dsJdbcUtil = dsJdbcUtil;
        this.cdcSchemaSyncService = cdcSchemaSyncService;
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

    /**
     * 通过名称获取数据源配置
     *
     */
    @Override
    public DsConfigBO findByName(String ds) {
        DsConfig dsConfig = dsConfigRepository.findByName(ds);
        return DsConfigAppConvert.INSTANCE.toDsConfigBO(dsConfig);
    }

    @Override
    @Transactional
    public DsConfigBO update(String dsName, DsConfigCmd cmd) {
        DsConfig existing = dsConfigRepository.findByName(dsName);
        Assert.notNull(existing, new SilentException("数据源不存在"));

        // 检查名称是否重复
        if (!existing.getName().equals(cmd.getName())) {
            DsConfig nameCheck = dsConfigRepository.find(new DsConfigFindQuery().setName(cmd.getName()));
            Assert.isNull(nameCheck, new SilentException("数据源名称已存在"));
        }

        DsConfig dsConfig = DsConfigAppConvert.INSTANCE.toDsConfig(cmd);
        dsConfig.setId(existing.getId());
        dsConfig.setCreateBy(existing.getCreateBy());
        dsConfig = dsConfig.update(dsConfigRepository);
        return DsConfigAppConvert.INSTANCE.toDsConfigBO(dsConfig);
    }

    @Override
    @Transactional
    public void delete(String dsName) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        dsConfig.delete(dsConfigRepository);
    }

    @Override
    public void testConnection(String dsName) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        dsJdbcUtil.testConnection(dsConfig);
    }

    @Override
    public DatasourceType getDatasourceType(String dsName) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        return dsConfig.getDatasourceType();
    }

    @Override
    public List<DatabaseValObj> listDatabases(String dsName) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        return dsJdbcUtil.listDatabases(dsConfig);
    }

    @Override
    public void createDatabase(String dsName, DatabaseCreateCmd cmd) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        dsJdbcUtil.createDatabase(dsConfig, cmd.getName(), cmd.getCharset(), cmd.getCollation());
    }

    @Override
    public List<TableSchemaValObj> listTables(String dsName, String dbName) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        return dsJdbcUtil.listTables(dsConfig, dbName);
    }

    @Override
    public TableSchemaValObj getTableSchema(String dsName, String dbName, String tableName) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        return dsJdbcUtil.getTableSchema(dsConfig, dbName, tableName);
    }

    @Override
    @Transactional
    public void createTable(String dsName, String dbName, TableSchemaCmd cmd) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
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
    public void updateTable(String dsName, String dbName, String tableName, TableSchemaCmd cmd) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        
        TableSchemaValObj tableSchema = new TableSchemaValObj()
                .setTableName(cmd.getTableName())
                .setTableComment(cmd.getTableComment())
                .setColumns(addDefaultColumns(dsConfig, cmd.getColumns()))
                .setIndexes(cmd.getIndexes());
        
        dsJdbcUtil.updateTable(dsConfig, dbName, tableName, tableSchema);

        // 异步触发 CDC Schema 同步（不阻塞前端响应）
        try {
            cdcSchemaSyncService.syncSchema(dsName, dbName, tableName, tableSchema);
        } catch (Exception e) {
            log.error("CDC Schema 同步失败: {}.{}.{}", dsName, dbName, tableName, e);
            // 同步失败不影响主流程，仅记录日志
        }
    }

    @Override
    @Transactional
    public void dropTable(String dsName, String dbName, String tableName) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        dsJdbcUtil.dropTable(dsConfig, dbName, tableName);
    }

    @Override
    public Map<String, Object> executeSql(String dsName, String dbName, String sql, Integer limit) {
        DsConfig dsConfig = dsConfigRepository.findByName(dsName);
        Assert.notNull(dsConfig, new SilentException("数据源不存在"));
        return dsJdbcUtil.executeSql(dsConfig, dbName, sql, limit);
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
