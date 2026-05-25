package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.dto.RelationVectorIndexResultDTO;
import com.cyan.dataman.application.metadata.RelationVectorIndexService;
import com.cyan.dataman.application.metadata.bo.RelationVectorIndexResultBO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 元数据表向量索引控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata/tables")
public class MetadataTableVectorIndexController {

    private final RelationVectorIndexService relationVectorIndexService;

    public MetadataTableVectorIndexController(RelationVectorIndexService relationVectorIndexService) {
        this.relationVectorIndexService = relationVectorIndexService;
    }

    /**
     * 全量重建向量索引
     *
     * @return 重建结果
     */
    @PostMapping("/vector-index/rebuild")
    public Response<RelationVectorIndexResultDTO> rebuildAll() {
        return Response.success(toDTO(relationVectorIndexService.rebuildAll()));
    }

    /**
     * 重建单表向量索引
     *
     * @param id 表ID
     * @return 重建结果
     */
    @PostMapping("/{id}/vector-index/rebuild")
    public Response<RelationVectorIndexResultDTO> rebuildOne(@PathVariable String id) {
        return Response.success(toDTO(relationVectorIndexService.rebuildOne(id)));
    }

    /**
     * 删除单表向量索引
     *
     * @param id 表ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}/vector-index")
    public Response<RelationVectorIndexResultDTO> deleteOne(@PathVariable String id) {
        return Response.success(toDTO(relationVectorIndexService.deleteOne(id)));
    }

    /**
     * 转换为 DTO
     */
    private RelationVectorIndexResultDTO toDTO(RelationVectorIndexResultBO bo) {
        return new RelationVectorIndexResultDTO()
                .setTotalCount(bo.getTotalCount())
                .setSuccessCount(bo.getSuccessCount())
                .setFailedCount(bo.getFailedCount());
    }
}
