package com.cyan.dataman.adapter.http.datasource;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.http.datasource.convert.DatasourceAdapterConvert;
import com.cyan.dataman.adapter.http.datasource.dto.DatasourceSchemaDTO;
import com.cyan.dataman.application.datasource.DatasourceService;
import com.cyan.dataman.application.datasource.bo.DatasourceSchemaBO;
import com.cyan.dataman.enums.StorageType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 查询数据库
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/datasource")
public class DatasourceController {
    private final DatasourceService datasourceService;

    public DatasourceController(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    /**
     * 获取数据库列表
     */
    @GetMapping("/schemas")
    public Response<List<DatasourceSchemaDTO>> schemas(StorageType storageType) {
        List<DatasourceSchemaBO> datasourceSchemaBOS = datasourceService.listSchemas(storageType);
        List<DatasourceSchemaDTO> list = Optional.ofNullable(datasourceSchemaBOS).orElse(List.of()).stream().map(DatasourceAdapterConvert.INSTANCE::toDatasourceSchemaDTO).toList();
        return Response.success(list);
    }
}
