package com.cyan.dataman.adapter.metadata.lineage.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.lineage.convert.MetadataLineageAdapterConvert;
import com.cyan.dataman.adapter.metadata.lineage.dto.MetadataFieldLineageDTO;
import com.cyan.dataman.application.metadata.lineage.MetadataLineageService;
import com.cyan.dataman.application.metadata.lineage.bo.MetadataFieldLineageBO;
import com.cyan.dataman.domain.metadata.lineage.query.MetadataFieldLineageQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 元数据血缘 HTTP 控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata/lineage")
public class MetadataLineageController {

    private final MetadataLineageService metadataLineageService;

    public MetadataLineageController(MetadataLineageService metadataLineageService) {
        this.metadataLineageService = metadataLineageService;
    }

    /**
     * 查询字段血缘
     */
    @GetMapping("/fields")
    public Response<MetadataFieldLineageDTO> queryFieldLineage(@RequestParam String catalog,
                                                               @RequestParam String schema,
                                                               @RequestParam String table,
                                                               @RequestParam String column,
                                                               @RequestParam(required = false, defaultValue = "3") Integer maxDepth) {
        MetadataFieldLineageBO bo = metadataLineageService.queryFieldLineage(new MetadataFieldLineageQuery()
                .setCatalog(catalog)
                .setSchema(schema)
                .setTable(table)
                .setColumn(column)
                .setMaxDepth(maxDepth));
        return Response.success(MetadataLineageAdapterConvert.INSTANCE.toDTO(bo));
    }
}
