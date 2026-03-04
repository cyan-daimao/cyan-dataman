package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.convert.MetadataTableAdapterConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataTableDTO;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.domain.metadata.query.MetadataTablePageQuery;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 *
 * 元数据接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata/tables")
public class MetadataTableController {
    private final MetadataTableService metadataTableService;

    public MetadataTableController(MetadataTableService metadataTableService) {
        this.metadataTableService = metadataTableService;
    }

    /**
     * 获取表列表
     *
     * @param content     搜索内容可以是表名或表描述
     * @param subjectCode 主题编码
     * @param owner       负责人
     */
    @GetMapping
    public Response<Page<MetadataTableDTO>> page(@RequestParam(required = false) String content,
                                                 @RequestParam(required = false) String subjectCode,
                                                 @RequestParam(required = false) String owner) {
        MetadataTablePageQuery query = new MetadataTablePageQuery()
                .setOrName(content)
                .setOrComment(content)
                .setSubjectCode(subjectCode)
                .setOwner(owner);
        Page<MetadataTableBO> metadataTables = metadataTableService.page(query);
        List<MetadataTableDTO> data = Optional.ofNullable(metadataTables.getData()).orElse(List.of()).stream().map(MetadataTableAdapterConvert.INSTANCE::toMetadataTableDTO).toList();
        Page<MetadataTableDTO> page = new Page<>(data, metadataTables.getCurrent(), metadataTables.getSize(), metadataTables.getTotal());
        return Response.success(page);
    }

    /**
     * 获取表
     */
    @GetMapping("/{id}")
    public Response<MetadataTableDTO> findById(@PathVariable String id) {
        MetadataTableBO metadataTable = metadataTableService.findById(id);
        MetadataTableDTO metadataTableDTO = MetadataTableAdapterConvert.INSTANCE.toMetadataTableDTO(metadataTable);
        return Response.success(metadataTableDTO);
    }



}
