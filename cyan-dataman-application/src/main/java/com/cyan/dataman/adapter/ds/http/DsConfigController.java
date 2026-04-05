package com.cyan.dataman.adapter.ds.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.ds.http.convert.DsConfigAdapterConvert;
import com.cyan.dataman.adapter.ds.http.dto.DatabaseDTO;
import com.cyan.dataman.adapter.ds.http.dto.DsConfigDTO;
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
    public Response<DsConfigDTO> findById(@PathVariable("ds") String dsId) {
        DsConfigBO bo = dsConfigService.findById(dsId);
        return Response.success(DsConfigAdapterConvert.INSTANCE.toDsConfigDTO(bo));
    }

    /**
     * 更新数据源配置
     */
    @PutMapping("/{ds}")
    public Response<DsConfigDTO> update(@PathVariable("ds") String dsId, @RequestBody @Valid DsConfigCmd cmd) {
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
        DsConfigBO bo = dsConfigService.update(dsId, cmd);
        return Response.success(DsConfigAdapterConvert.INSTANCE.toDsConfigDTO(bo));
    }

    /**
     * 删除数据源配置
     */
    @DeleteMapping("/{ds}")
    public Response<Void> delete(@PathVariable("ds") String dsId) {
        dsConfigService.delete(dsId);
        return Response.success();
    }

    /**
     * 测试数据源连接
     */
    @PostMapping("/{ds}/test")
    public Response<Void> testConnection(@PathVariable("ds") String dsId) {
        dsConfigService.testConnection(dsId);
        return Response.success();
    }

    // ==================== 数据库管理 ====================

    /**
     * 获取数据源下的数据库列表
     */
    @GetMapping("/{ds}/dbs")
    public Response<List<DatabaseDTO>> listDatabases(@PathVariable("ds") String dsId) {
        List<DatabaseValObj> databases = dsConfigService.listDatabases(dsId);
        List<DatabaseDTO> dtos = Optional.ofNullable(databases).orElse(List.of()).stream()
                .map(DsConfigAdapterConvert.INSTANCE::toDatabaseDTO)
                .toList();
        return Response.success(dtos);
    }

    /**
     * 创建数据库
     */
    @PostMapping("/{ds}/dbs")
    public Response<Void> createDatabase(@PathVariable("ds") String dsId, @RequestBody DatabaseCreateCmd cmd) {
        dsConfigService.createDatabase(dsId, cmd);
        return Response.success();
    }

    // ==================== 表管理 ====================

    /**
     * 获取数据库下的表列表
     */
    @GetMapping("/{ds}/dbs/{db}/tables")
    public Response<List<String>> listTables(
            @PathVariable("ds") String dsId,
            @PathVariable("db") String dbName) {
        List<String> tables = dsConfigService.listTables(dsId, dbName);
        return Response.success(tables);
    }

    /**
     * 获取表详情
     */
    @GetMapping("/{ds}/dbs/{db}/tables/{tbl}")
    public Response<TableSchemaDTO> getTableSchema(
            @PathVariable("ds") String dsId,
            @PathVariable("db") String dbName,
            @PathVariable("tbl") String tableName) {
        TableSchemaValObj schema = dsConfigService.getTableSchema(dsId, dbName, tableName);
        return Response.success(DsConfigAdapterConvert.INSTANCE.toTableSchemaDTO(schema));
    }

    /**
     * 创建表
     */
    @PostMapping("/{ds}/dbs/{db}/tables")
    public Response<Void> createTable(
            @PathVariable("ds") String dsId,
            @PathVariable("db") String dbName,
            @RequestBody @Valid TableSchemaCmdDTO cmd) {
        DatasourceType dsType = dsConfigService.getDatasourceType(dsId);
        TableSchemaCmd schemaCmd = convertToTableSchemaCmd(cmd, dsType);
        dsConfigService.createTable(dsId, dbName, schemaCmd);
        return Response.success();
    }

    /**
     * 更新表结构
     */
    @PutMapping("/{ds}/dbs/{db}/tables/{tbl}")
    public Response<Void> updateTable(
            @PathVariable("ds") String dsId,
            @PathVariable("db") String dbName,
            @PathVariable("tbl") String tableName,
            @RequestBody @Valid TableSchemaCmdDTO cmd) {
        DatasourceType dsType = dsConfigService.getDatasourceType(dsId);
        TableSchemaCmd schemaCmd = convertToTableSchemaCmd(cmd, dsType);
        dsConfigService.updateTable(dsId, dbName, tableName, schemaCmd);
        return Response.success();
    }

    /**
     * 删除表
     */
    @DeleteMapping("/{ds}/dbs/{db}/tables/{tbl}")
    public Response<Void> dropTable(
            @PathVariable("ds") String dsId,
            @PathVariable("db") String dbName,
            @PathVariable("tbl") String tableName) {
        dsConfigService.dropTable(dsId, dbName, tableName);
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
