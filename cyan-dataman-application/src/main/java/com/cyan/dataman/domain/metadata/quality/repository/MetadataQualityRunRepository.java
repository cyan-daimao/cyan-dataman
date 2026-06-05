package com.cyan.dataman.domain.metadata.quality.repository;

import com.cyan.dataman.domain.metadata.quality.MetadataQualityRun;

import java.util.List;

/**
 * 元数据质量运行仓库
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataQualityRunRepository {

    /**
     * 保存运行记录
     */
    MetadataQualityRun save(MetadataQualityRun run);

    /**
     * 更新运行记录
     */
    MetadataQualityRun update(MetadataQualityRun run);

    /**
     * 根据ID查询运行记录
     */
    MetadataQualityRun findById(Long id);

    /**
     * 查询表运行记录
     */
    List<MetadataQualityRun> listByTableId(String tableId, Integer limit);

    /**
     * 查询表最近一次运行
     */
    MetadataQualityRun findLatestByTableId(String tableId);
}
