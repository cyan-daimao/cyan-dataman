package com.cyan.dataman.adapter.http.bigdata.table;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.dataman.adapter.http.bigdata.table.dto.TableMetaDTO;
import com.cyan.dataman.adapter.http.bigdata.table.dto.TableSnapshotsDTO;
import com.cyan.dataman.adapter.http.bigdata.table.convert.TableMetaAdapterConvert;
import com.cyan.dataman.application.bigdata.table.bo.TableMetaBO;
import com.cyan.dataman.application.bigdata.table.service.TableMetaService;
import com.cyan.dataman.domain.bigdata.table.cmd.TableMetaCmd;
import com.cyan.dataman.domain.bigdata.table.query.TableDataListQuery;
import com.cyan.dataman.domain.bigdata.table.query.TableQuery;
import com.cyan.dataman.enums.DataLayer;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
     */
    @GetMapping
    public Response<TableMetaDTO> get(@RequestParam DataLayer db, @RequestParam String name) {
        TableMetaBO table = tableMetaService.get(db, name);
        return Response.success(TableMetaAdapterConvert.INSTANCE.toTableDTO(table));
    }

    /**
     * 获取表列表
     */
    @GetMapping("/list")
    public Response<Page<TableMetaDTO>> list(TableQuery query) {
        query.setCurrent(query.getCurrent() <= 0 ? 1 : query.getCurrent());
        query.setSize(query.getSize() <= 0 ? 10 : query.getSize());
        List<TableMetaBO> tableMetaBOS = tableMetaService.listTableByDb(Arrays.stream(DataLayer.values()).toList());
        List<TableMetaDTO> list = Optional.ofNullable(tableMetaBOS).orElse(List.of()).stream().map(TableMetaAdapterConvert.INSTANCE::toTableDTO)
                .filter(table -> {
                    if (query.getDb() != null) {
                        if (!query.getDb().equals(table.getDb())) {
                            return false;
                        }
                    }
                    if (StrUtils.isNotBlank(query.getName())) {
                        return table.getName().contains(query.getName());
                    }
                    return true;
                }).toList();
        List<TableMetaDTO> data = list.stream().skip((query.getCurrent() - 1) * query.getSize())
                .limit(query.getSize())
                .toList();
        Page<TableMetaDTO> page = new Page<TableMetaDTO>()
                .setData(data)
                .setCurrent(query.getCurrent())
                .setSize(query.getSize())
                .setTotal(list.size());

        return Response.success(page);
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
        TableMetaBO tableMetaBO = tableMetaService.snapshots(query.getDb(), query.getName());
        List<TableSnapshotsDTO> list = Optional.ofNullable(tableMetaBO.getSnapshots()).orElse(List.of()).stream().map(TableMetaAdapterConvert.INSTANCE::toTableSnapshotsDTO).toList().reversed();
        return Response.success(list);
    }
}
