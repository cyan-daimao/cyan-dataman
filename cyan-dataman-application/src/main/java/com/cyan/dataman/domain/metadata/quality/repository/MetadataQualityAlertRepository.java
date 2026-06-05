package com.cyan.dataman.domain.metadata.quality.repository;

import com.cyan.dataman.domain.metadata.quality.MetadataQualityAlert;

import java.util.List;

/**
 * 元数据质量告警仓库
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataQualityAlertRepository {

    /**
     * 保存告警
     */
    MetadataQualityAlert save(MetadataQualityAlert alert);

    /**
     * 更新告警
     */
    MetadataQualityAlert update(MetadataQualityAlert alert);

    /**
     * 根据ID查询告警
     */
    MetadataQualityAlert findById(Long id);

    /**
     * 查询表告警
     */
    List<MetadataQualityAlert> listByTableId(String tableId);
}
