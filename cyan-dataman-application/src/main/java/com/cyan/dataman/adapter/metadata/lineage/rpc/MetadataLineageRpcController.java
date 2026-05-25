package com.cyan.dataman.adapter.metadata.lineage.rpc;

import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.lineage.convert.MetadataLineageAdapterConvert;
import com.cyan.dataman.application.metadata.lineage.MetadataLineageService;
import com.cyan.dataman.application.metadata.lineage.cmd.MetadataLineageSyncCmd;
import com.cyan.dataman.client.lineage.MetadataLineageClient;
import com.cyan.dataman.client.lineage.request.MetadataLineageSyncRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 元数据血缘 RPC 控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/rpc/v1/metadata/lineage")
@RequiredArgsConstructor
public class MetadataLineageRpcController implements MetadataLineageClient {

    private final MetadataLineageService metadataLineageService;


    /**
     * 同步血缘节点与边
     */
    @Override
    @PostMapping("/sync")
    public Response<Void> sync(@RequestBody MetadataLineageSyncRequest request) {
        MetadataLineageSyncCmd cmd = new MetadataLineageSyncCmd()
                .setServiceName(request.getServiceName())
                .setRefId(request.getRefId())
                .setNodes(MetadataLineageAdapterConvert.INSTANCE.toNodeCmds(request.getNodes()))
                .setEdges(MetadataLineageAdapterConvert.INSTANCE.toEdgeCmds(request.getEdges()));
        metadataLineageService.sync(cmd);
        return Response.success();
    }
}
