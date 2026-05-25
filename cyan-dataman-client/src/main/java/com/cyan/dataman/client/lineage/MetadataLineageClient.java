package com.cyan.dataman.client.lineage;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.client.lineage.request.MetadataLineageSyncRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 元数据血缘 RPC 客户端
 *
 * @author cy.Y
 * @since 1.0.0
 */
@FeignClient(name = "cyan-dataman", contextId = "metadataLineageClient", path = "/rpc/v1/metadata/lineage", url = "${feign.cyan-dataman.url:}")
public interface MetadataLineageClient {

    /**
     * 同步血缘节点与边
     *
     * @param request 血缘同步请求
     * @return 同步结果
     */
    @PostMapping("/sync")
    Response<Void> sync(@RequestBody MetadataLineageSyncRequest request);
}
