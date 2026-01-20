package com.cyan.dataman.adapter.http.bigdata.table;

import com.cyan.arch.common.api.ErrorCode;
import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataman.adapter.http.bigdata.table.convert.TableMetaAdapterConvert;
import com.cyan.dataman.adapter.http.bigdata.table.dto.TableMetaDTO;
import com.cyan.dataman.adapter.http.bigdata.table.dto.TableSnapshotsDTO;
import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.application.bigdata.table.service.TableMetaService;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaCmd;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaDeleteCmd;
import com.cyan.dataman.domain.bigdata.table.query.TableDataListQuery;
import com.cyan.dataman.domain.bigdata.table.query.TableQuery;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 元数据-表接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/meta/table")
public class TableMetaController {

    private final TableMetaService tableMetaService;

    public TableMetaController(TableMetaService tableMetaService) {
        this.tableMetaService = tableMetaService;
    }

    /**
     * 获取表信息
     *
     * @param catalog 目录
     * @param db      库名
     * @param tbl     表名
     */
    @GetMapping
    public Response<TableMetaDTO> get(@RequestParam String catalog, @RequestParam String db, @RequestParam String tbl) {
        TableMetaBO table = tableMetaService.get(catalog, db, tbl);
        if (table == null) {
            return Response.failed(ErrorCode.FAILED, "表不存在");
        }
        return Response.success(TableMetaAdapterConvert.INSTANCE.toTableDTO(table));
    }

    /**
     * 获取表列表
     */
    @GetMapping("/list")
    public Response<List<TableMetaDTO>> list(TableQuery query) {
        query.setCurrent(query.getCurrent() <= 0 ? 1 : query.getCurrent());
        query.setSize(query.getSize() <= 0 ? 10 : query.getSize());
        List<String> dbs = StrUtils.isBlank(query.getDb()) ? null : List.of(query.getDb());
        List<TableMetaBO> tableMetaBOS = tableMetaService.listTableByDb(query.getCatalog(), dbs);
        List<TableMetaDTO> data = Optional.ofNullable(tableMetaBOS).orElse(List.of()).stream().map(TableMetaAdapterConvert.INSTANCE::toTableDTO).toList();
        return Response.success(data);
    }


    /**
     * 创建表
     */
    @PostMapping("/create")
    public Response<TableMetaDTO> create(@RequestBody @Validated TableMetaCmd tableMetaCmd) {
        TableMetaBO tableMetaBO = tableMetaService.create(tableMetaCmd);
        TableMetaDTO tableMetaDTO = TableMetaAdapterConvert.INSTANCE.toTableDTO(tableMetaBO);
        return Response.success(tableMetaDTO);
    }

    /**
     * 获取表快照数据
     */
    @GetMapping("/snapshots")
    public Response<List<TableSnapshotsDTO>> snapshots(@Validated TableDataListQuery query) {
        TableMetaBO tableMetaBO = tableMetaService.snapshots(query.getCatalog(), query.getDb(), query.getName());
        List<TableSnapshotsDTO> list = Optional.ofNullable(tableMetaBO.getSnapshots()).orElse(List.of()).stream().map(TableMetaAdapterConvert.INSTANCE::toTableSnapshotsDTO).toList().reversed();
        return Response.success(list);
    }

    /**
     * 删除表
     */
    @PostMapping("/delete")
    public Response<Void> delete(@RequestBody @Validated TableMetaDeleteCmd cmd) {
        tableMetaService.delete(cmd);
        return Response.success();
    }
}
