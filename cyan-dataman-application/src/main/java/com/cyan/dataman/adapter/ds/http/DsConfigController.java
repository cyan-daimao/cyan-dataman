package com.cyan.dataman.adapter.ds.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.ds.http.convert.DsConfigAdapterConvert;
import com.cyan.dataman.adapter.ds.http.dto.DatabaseDTO;
import com.cyan.dataman.adapter.ds.http.dto.DsConfigDTO;
import com.cyan.dataman.adapter.ds.http.dto.SqlExecuteCmdDTO;
import com.cyan.dataman.adapter.ds.http.dto.SqlResultDTO;
import com.cyan.dataman.adapter.ds.http.dto.TableSchemaCmdDTO;
import com.cyan.dataman.adapter.ds.http.dto.TableSchemaDTO;
import com.cyan.dataman.application.ds.DsConfigService;
import com.cyan.dataman.application.ds.bo.DsConfigBO;
import com.cyan.dataman.application.ds.cmd.DatabaseCreateCmd;
import com.cyan.dataman.application.ds.cmd.DsConfigCmd;
import com.cyan.dataman.application.ds.cmd.TableSchemaCmd;
import com.cyan.dataman.domain.ds.query.DsConfigListQuery;
import com.cyan.dataman.domain.ds.valobj.DatabaseValObj;
import com.cyan.dataman.domain.ds.valobj.TableSchemaValObj;
import com.cyan.dataman.enums.DatasourceType;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据源配置控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/ds")
public class DsConfigController {

    private final DsConfigService dsConfigService;

    public DsConfigController(DsConfigService dsConfigService) {
        this.dsConfigService = dsConfigService;
    }

    // ==================== 数据源管理 ====================

    /**
     * 创建数据源配置
     */
    @PostMapping
    public Response<DsConfigDTO> create(@RequestBody @Valid DsConfigCmd cmd) {
        cmd.setCreateBy(UserContextHolder.getCurrentEmployee().getPassport());
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
        DsConfigBO bo = dsConfigService.create(cmd);
        return Response.success(DsConfigAdapterConvert.INSTANCE.toDsConfigDTO(bo));
    }

    /**
     * 获取数据源配置列表
     */
    @GetMapping
    public Response<List<DsConfigDTO>> list(DsConfigListQuery query) {
        List<DsConfigBO> list = dsConfigService.list(query);
        List<DsConfigDTO> dtos = Optional.ofNullable(list).orElse(List.of()).stream()
                .map(DsConfigAdapterConvert.INSTANCE::toDsConfigDTO)
                .toList();
        return Response.success(dtos);
    }

    /**
     * 获取数据源配置
     */
    @GetMapping("/{ds}")
    public Response<DsConfigDTO> findByName(@PathVariable("ds") String ds) {
        DsConfigBO bo = dsConfigService.findByName(ds);
        return Response.success(DsConfigAdapterConvert.INSTANCE.toDsConfigDTO(bo));
    }

    /**
     * 更新数据源配置
     */
    @PutMapping("/{dsName}")
    public Response<DsConfigDTO> update(@PathVariable("dsName") String dsName, @RequestBody @Valid DsConfigCmd cmd) {
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
        DsConfigBO bo = dsConfigService.update(dsName, cmd);
        return Response.success(DsConfigAdapterConvert.INSTANCE.toDsConfigDTO(bo));
    }

    /**
     * 删除数据源配置
     */
    @DeleteMapping("/{dsName}")
    public Response<Void> delete(@PathVariable("dsName") String dsName) {
        dsConfigService.delete(dsName);
        return Response.success();
    }

    /**
     * 测试数据源连接
     */
    @PostMapping("/{dsName}/test")
    public Response<Void> testConnection(@PathVariable("dsName") String dsName) {
        dsConfigService.testConnection(dsName);
        return Response.success();
    }

    // ==================== 数据库管理 ====================

    /**
     * 获取数据源下的数据库列表
     */
    @GetMapping("/{dsName}/dbs")
    public Response<List<DatabaseDTO>> listDatabases(@PathVariable("dsName") String dsName) {
        List<DatabaseValObj> databases = dsConfigService.listDatabases(dsName);
        List<DatabaseDTO> dtos = Optional.ofNullable(databases).orElse(List.of()).stream()
                .map(DsConfigAdapterConvert.INSTANCE::toDatabaseDTO)
                .toList();
        return Response.success(dtos);
    }

    /**
     * 创建数据库
     */
    @PostMapping("/{dsName}/dbs")
    public Response<Void> createDatabase(@PathVariable("dsName") String dsName, @RequestBody DatabaseCreateCmd cmd) {
        dsConfigService.createDatabase(dsName, cmd);
        return Response.success();
    }

    // ==================== SQL 执行 ====================

    /**
     * 执行 SQL 语句（自动判断 DQL 或 DML）
     */
    @PostMapping("/{dsName}/dbs/{db}/execute")
    public Response<SqlResultDTO> executeSql(
            @PathVariable("dsName") String dsName,
            @PathVariable("db") String dbName,
            @RequestBody @Valid SqlExecuteCmdDTO cmd) {
        Map<String, Object> result = dsConfigService.executeSql(dsName, dbName, cmd.getSql(), cmd.getLimit());
        
        Boolean isQuery = (Boolean) result.get("isQuery");
        if (Boolean.TRUE.equals(isQuery)) {
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) result.get("columns");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("rows");
            Integer rowCount = (Integer) result.get("rowCount");
            return Response.success(SqlResultDTO.ofQuery(columns, rows, rowCount));
        } else {
            Integer affectedRows = (Integer) result.get("affectedRows");
            return Response.success(SqlResultDTO.ofDml(affectedRows));
        }
    }

    // ==================== 表管理 ====================

    /**
     * 获取数据库下的表列表
     */
    @GetMapping("/{dsName}/dbs/{db}/tables")
    public Response<List<TableSchemaDTO>> listTables(
            @PathVariable("dsName") String dsName,
            @PathVariable("db") String dbName) {
        List<TableSchemaValObj> tables = dsConfigService.listTables(dsName, dbName);
        List<TableSchemaDTO> dtos = Optional.ofNullable(tables).orElse(List.of()).stream()
                .map(DsConfigAdapterConvert.INSTANCE::toTableSchemaDTO)
                .toList();
        return Response.success(dtos);
    }

    /**
     * 获取表详情
     */
    @GetMapping("/{dsName}/dbs/{db}/tables/{tbl}")
    public Response<TableSchemaDTO> getTableSchema(
            @PathVariable("dsName") String dsName,
            @PathVariable("db") String dbName,
            @PathVariable("tbl") String tableName) {
        TableSchemaValObj schema = dsConfigService.getTableSchema(dsName, dbName, tableName);
        return Response.success(DsConfigAdapterConvert.INSTANCE.toTableSchemaDTO(schema));
    }

    /**
     * 创建表
     */
    @PostMapping("/{dsName}/dbs/{db}/tables")
    public Response<Void> createTable(
            @PathVariable("dsName") String dsName,
            @PathVariable("db") String dbName,
            @RequestBody @Valid TableSchemaCmdDTO cmd) {
        DatasourceType dsType = dsConfigService.getDatasourceType(dsName);
        TableSchemaCmd schemaCmd = convertToTableSchemaCmd(cmd, dsType);
        dsConfigService.createTable(dsName, dbName, schemaCmd);
        return Response.success();
    }

    /**
     * 更新表结构
     */
    @PutMapping("/{dsName}/dbs/{db}/tables/{tbl}")
    public Response<Void> updateTable(
            @PathVariable("dsName") String dsName,
            @PathVariable("db") String dbName,
            @PathVariable("tbl") String tableName,
            @RequestBody @Valid TableSchemaCmdDTO cmd) {
        DatasourceType dsType = dsConfigService.getDatasourceType(dsName);
        TableSchemaCmd schemaCmd = convertToTableSchemaCmd(cmd, dsType);
        dsConfigService.updateTable(dsName, dbName, tableName, schemaCmd);
        return Response.success();
    }

    /**
     * 删除表
     */
    @DeleteMapping("/{dsName}/dbs/{db}/tables/{tbl}")
    public Response<Void> dropTable(
            @PathVariable("dsName") String dsName,
            @PathVariable("db") String dbName,
            @PathVariable("tbl") String tableName) {
        dsConfigService.dropTable(dsName, dbName, tableName);
        return Response.success();
    }

    // ==================== 私有方法 ====================

    /**
     * 将 TableSchemaCmdDTO 转换为 TableSchemaCmd
     */
    private TableSchemaCmd convertToTableSchemaCmd(TableSchemaCmdDTO dto, DatasourceType dsType) {
        return new TableSchemaCmd()
                .setTableName(dto.getTableName())
                .setTableComment(dto.getTableComment())
                .setColumns(DsConfigAdapterConvert.INSTANCE.toColumnValObjList(dto.getColumns(), dsType))
                .setIndexes(DsConfigAdapterConvert.INSTANCE.toIndexValObjList(dto.getIndexes()));
    }
}
