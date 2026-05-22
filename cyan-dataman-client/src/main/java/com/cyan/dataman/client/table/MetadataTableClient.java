package com.cyan.dataman.client.table;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.table.dto.MetadataTableDTO;
import com.cyan.dataman.client.table.request.MetadataTableCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 元数据表 RPC Feign 客户端
 * <p>
 * 供其他微服务调用，路径为 /rpc/v1/metadata/tables，不依赖登录态。
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataman", contextId = "metadataTableClient", path = "/rpc/v1/metadata/tables", url = "${feign.cyan-dataman.url:}")
public interface MetadataTableClient {

    /**
     * 创建表
     */
    @PostMapping
    Response<MetadataTableDTO> save(@RequestBody MetadataTableCreateRequest request);

    /**
     * 根据表名查询表
     */
    @GetMapping("/by-name/{name}")
    Response<MetadataTableDTO> getByName(@PathVariable("name") String name);
}
