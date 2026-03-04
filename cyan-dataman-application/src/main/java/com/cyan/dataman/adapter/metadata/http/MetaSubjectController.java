package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.convert.MetadataSubjectAdapterConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataSubjectDTO;
import com.cyan.dataman.application.metadata.MetadataSubjectService;
import com.cyan.dataman.application.metadata.bo.MetadataSubjectBO;
import com.cyan.dataman.application.metadata.cmd.MetadataSubjectCmd;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 *
 * 主题接口
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata/subjects")
public class MetaSubjectController {

    private final MetadataSubjectService metadataSubjectService;

    public MetaSubjectController(MetadataSubjectService metadataSubjectService) {
        this.metadataSubjectService = metadataSubjectService;
    }

    /**
     * 获取主题列表
     */
    @GetMapping
    public Response<List<MetadataSubjectDTO>> list() {
        List<MetadataSubjectBO> subjectBOS = metadataSubjectService.list();
        List<MetadataSubjectDTO> list = Optional.ofNullable(subjectBOS).orElse(List.of()).stream().map(MetadataSubjectAdapterConvert.INSTANCE::toDMetadataSubjectDTO).toList();
        return Response.success(list);
    }

    /**
     * 获取主题
     */
    @GetMapping("/{id}")
    public Response<MetadataSubjectDTO> findById(@PathVariable String id) {
        MetadataSubjectBO subjectBO = metadataSubjectService.findById(id);
        MetadataSubjectDTO metadataSubjectDTO = MetadataSubjectAdapterConvert.INSTANCE.toDMetadataSubjectDTO(subjectBO);
        return Response.success(metadataSubjectDTO);
    }


    /**
     * 创建主题
     */
    @PostMapping
    public Response<MetadataSubjectDTO> create(MetadataSubjectCmd cmd) {
        MetadataSubjectBO subjectBO = metadataSubjectService.create(cmd);
        MetadataSubjectDTO metadataSubjectDTO = MetadataSubjectAdapterConvert.INSTANCE.toDMetadataSubjectDTO(subjectBO);
        return Response.success(metadataSubjectDTO);
    }

    /**
     * 修改主题
     */
    @PutMapping("/{id}")
    public Response<MetadataSubjectDTO> update(@PathVariable String id, @RequestBody MetadataSubjectCmd cmd) {
        MetadataSubjectBO subjectBO = metadataSubjectService.update(id, cmd);
        MetadataSubjectDTO metadataSubjectDTO = MetadataSubjectAdapterConvert.INSTANCE.toDMetadataSubjectDTO(subjectBO);
        return Response.success(metadataSubjectDTO);
    }

    /**
     * 删除主题
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        metadataSubjectService.deleteById(id);
        return Response.success();
    }
}
