package com.cyan.dataman.application.metadata.lineage;

import com.cyan.dataman.application.metadata.lineage.bo.MetadataFieldLineageBO;
import com.cyan.dataman.application.metadata.lineage.cmd.MetadataLineageSyncCmd;
import com.cyan.dataman.domain.metadata.lineage.query.MetadataFieldLineageQuery;

/**
 * 元数据血缘应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataLineageService {

    /**
     * 同步血缘节点与边
     */
    void sync(MetadataLineageSyncCmd cmd);

    /**
     * 查询字段血缘
     */
    MetadataFieldLineageBO queryFieldLineage(MetadataFieldLineageQuery query);
}
