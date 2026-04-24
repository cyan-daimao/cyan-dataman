package com.cyan.dataman.client;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.dto.MetadataColumnDTO;
import com.cyan.dataman.client.dto.MetadataTableDTO;
import com.cyan.dataman.client.query.MetadataTableQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 元数据表 Feign Client
 *
 * @author cy.Y
 * @since 1.1.0
 */
@FeignClient(name = "cyan-dataman", path = "/api/v1/metadata/tables")
public interface DatamanTableClient {

    /**
     * 分页查询元数据表
     */
    @GetMapping
    Response<Page<MetadataTableDTO>> pageMetadataTables(@SpringQueryMap MetadataTableQuery query); // API: ready

    /**
     * 获取元数据表详情
     */
    @GetMapping("/{id}")
    Response<MetadataTableDTO> getMetadataTableById(@PathVariable("id") String id); // API: ready

    /**
     * 获取表字段列表
     */
    @GetMapping("/{id}/columns")
    Response<List<MetadataColumnDTO>> getTableColumns(@PathVariable("id") String id); // API: ready
}
