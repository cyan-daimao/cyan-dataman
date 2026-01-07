package com.cyan.dataman.adapter.http.datasource;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.http.datasource.convert.DatasourceAdapterConvert;
import com.cyan.dataman.adapter.http.datasource.dto.DatasourceSchemaDTO;
import com.cyan.dataman.adapter.http.datasource.dto.DatasourceTableDTO;
import com.cyan.dataman.application.datasource.DatasourceService;
import com.cyan.dataman.application.datasource.bo.DatasourceSchemaBO;
import com.cyan.dataman.application.datasource.bo.DatasourceTableBO;
import com.cyan.dataman.domain.datasource.query.DatasourceTableQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 数据源表接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/datasource")
public class DatasourceTableController {

    private final DatasourceService datasourceService;

    public DatasourceTableController(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    /**
     * 获取数据源-表列表
     */
    @GetMapping("/db/list")
    public Response<List<DatasourceSchemaDTO>> listDB(DatasourceTableQuery query) {
        List<DatasourceSchemaBO> list = datasourceService.listDB(query);
        List<DatasourceSchemaDTO> data = Optional.ofNullable(list).orElse(List.of()).stream().map(DatasourceAdapterConvert.INSTANCE::toDatasourceSchemaDTO).toList();
        return Response.success(data);
    }

    @GetMapping("/db/table/list")
    public Response<List<DatasourceTableDTO>> listTable(DatasourceTableQuery query) {
        List<DatasourceTableBO> list = datasourceService.listTable(query);
        List<DatasourceTableDTO> data = Optional.ofNullable(list).orElse(List.of()).stream().map(DatasourceAdapterConvert.INSTANCE::toDatasourceTableDTO).toList();
        return Response.success(data);
    }
}
