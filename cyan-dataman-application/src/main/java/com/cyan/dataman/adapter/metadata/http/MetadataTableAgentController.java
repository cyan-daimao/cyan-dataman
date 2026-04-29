package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataTableAgentDTO;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 元数据表智能体接口
 * @author cy.Y
 * @since v1.0.0
 */
@RestController
@RequestMapping("/api/v1/agent/meta/tables")
@RequiredArgsConstructor
public class MetadataTableAgentController {
    private MetadataTableService metadataTableService;

    /**
     * 获取元数据表列表
     * @return 元数据表列表
     */
    @GetMapping
    public Response<List<MetadataTableAgentDTO>> list(){
        List<MetadataTableBO> list = metadataTableService.list(new MetadataTableListQuery());
        return Response.success();
    }
}
