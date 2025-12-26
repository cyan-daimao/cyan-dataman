package com.cyan.dataman.adapter.http.datasource;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.http.datasource.convert.DatasourceAdapterConvert;
import com.cyan.dataman.adapter.http.datasource.dto.DatasourceTableDTO;
import com.cyan.dataman.application.datasource.DatasourceService;
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
@RequestMapping("/api/v1/datasource/table")
public class DatasourceTableController {

    private final DatasourceService datasourceService;

    public DatasourceTableController(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    @GetMapping("/list")
    public Response<List<DatasourceTableDTO>> list(DatasourceTableQuery query) {
        List<DatasourceTableBO> list = datasourceService.list(query);
        List<DatasourceTableDTO> data = Optional.ofNullable(list).orElse(List.of()).stream().map(DatasourceAdapterConvert.INSTANCE::toDatasourceTableDTO).toList();
        return Response.success(data);
    }
}
