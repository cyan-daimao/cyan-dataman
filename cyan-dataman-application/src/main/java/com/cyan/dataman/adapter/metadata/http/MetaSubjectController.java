package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.convert.MetadataSubjectAdapterConvert;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataSubjectDTO;
import com.cyan.dataman.application.metadata.MetadataSubjectService;
import com.cyan.dataman.application.metadata.bo.MetadataSubjectBO;
import com.cyan.dataman.application.metadata.cmd.MetadataSubjectCmd;
import com.cyan.dataman.domain.metadata.query.MetadataSubjectListQuery;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
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
    public Response<List<MetadataSubjectDTO>> list(MetadataSubjectListQuery query) {
        List<MetadataSubjectBO> subjectBOS = metadataSubjectService.list(query);
        List<MetadataSubjectDTO> list = Optional.ofNullable(subjectBOS).orElse(List.of()).stream().map(MetadataSubjectAdapterConvert.INSTANCE::toDMetadataSubjectDTO).toList();
        return Response.success(list);
    }

    /**
     * 获取主题树
     */
    @GetMapping("/tree")
    public Response<List<MetadataSubjectDTO>> tree() {
        List<MetadataSubjectBO> subjectBOS = metadataSubjectService.list(null);
        subjectBOS = MetadataSubjectBO.buildTree(subjectBOS);
        List<MetadataSubjectDTO> tree = subjectBOS.stream().map(MetadataSubjectAdapterConvert.INSTANCE::toDMetadataSubjectDTO).toList();
        return Response.success(tree);
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
    public Response<MetadataSubjectDTO> create(@RequestBody @Valid MetadataSubjectCmd cmd) {
        cmd.setParentId(StringUtils.isBlank(cmd.getParentId())?"0":cmd.getParentId());
        cmd.setCreateBy(UserContextHolder.getCurrentEmployee().getPassport());
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
        cmd.setOwner(cmd.getOwner());
        MetadataSubjectBO subjectBO = metadataSubjectService.create(cmd);
        MetadataSubjectDTO metadataSubjectDTO = MetadataSubjectAdapterConvert.INSTANCE.toDMetadataSubjectDTO(subjectBO);
        return Response.success(metadataSubjectDTO);
    }

    /**
     * 修改主题
     */
    @PutMapping("/{id}")
    public Response<MetadataSubjectDTO> update(@PathVariable String id, @RequestBody MetadataSubjectCmd cmd) {
        cmd.setUpdateBy(UserContextHolder.getCurrentEmployee().getPassport());
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
