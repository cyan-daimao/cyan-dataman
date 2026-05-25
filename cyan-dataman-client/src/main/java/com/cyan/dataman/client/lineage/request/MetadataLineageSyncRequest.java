package com.cyan.dataman.client.lineage.request;

import com.cyan.dataman.client.lineage.dto.MetadataLineageEdgeDTO;
import com.cyan.dataman.client.lineage.dto.MetadataLineageNodeDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 元数据血缘同步请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MetadataLineageSyncRequest {

    /**
     * 来源服务名
     */
    private String serviceName;

    /**
     * 来源业务ID
     */
    private String refId;

    /**
     * 血缘节点列表
     */
    private List<MetadataLineageNodeDTO> nodes;

    /**
     * 血缘边列表
     */
    private List<MetadataLineageEdgeDTO> edges;
}
