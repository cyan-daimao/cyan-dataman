package com.cyan.dataman.adapter.metadata.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataColumnAgentDTO;
import com.cyan.dataman.adapter.metadata.http.dto.MetadataTableAgentDTO;
import com.cyan.dataman.adapter.metadata.rpc.convert.MetadataTableAgentRPCConvert;
import com.cyan.dataman.application.metadata.MetadataTableService;
import com.cyan.dataman.application.metadata.bo.MetadataColumnBO;
import com.cyan.dataman.application.metadata.bo.MetadataTableBO;
import com.cyan.dataman.domain.metadata.query.MetadataTableListQuery;
import com.cyan.dataman.domain.metadata.query.MetadataTableOneQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据表智能体接口
 *
 * @author cy.Y
 * @since v1.0.0
 */
@RestController
@RequestMapping("/rpc/v1/agent/meta/tables")
@RequiredArgsConstructor
public class MetadataTableAgentRPC {
    private final MetadataTableService metadataTableService;

    /**
     * 获取表字段列表
     */
    @GetMapping("{tableName}/columns")
    public Response<List<MetadataColumnAgentDTO>> getTableColumns(@PathVariable String tableName) {
        MetadataTableBO table = metadataTableService.findOne(new MetadataTableOneQuery().setName(tableName));
        if (table == null) {
            throw new SilentException("表不存在: " + tableName);
        }
        List<MetadataColumnBO> columnBOs = metadataTableService.listColumns(table.getId());
        List<MetadataColumnAgentDTO> cols = MetadataTableAgentRPCConvert.INSTANCE.toMetadataColumnAgentDTOList(columnBOs);
        return Response.success(cols);
    }

    /**
     * 获取元数据表列表
     * @param keyword  搜索关键词（同时匹配表名或表描述），为空时返回全部
     * @return 元数据表列表
     */
    @GetMapping
    public Response<List<MetadataTableAgentDTO>> list(@RequestParam(value = "keyword", required = false) String keyword){
        MetadataTableListQuery query = new MetadataTableListQuery();
        if (keyword != null && !keyword.isBlank()) {
            query.setContent(keyword);
        }
        List<MetadataTableBO> list = metadataTableService.list(query);
        List<MetadataTableAgentDTO> data = MetadataTableAgentRPCConvert.INSTANCE.toMetadataTableAgentDTOList(list);
        return Response.success(data);
    }
}
