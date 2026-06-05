package com.cyan.dataman.domain.metadata.quality.repository;

import com.cyan.dataman.domain.metadata.quality.MetadataQualityResult;

import java.util.List;

/**
 * 元数据质量结果仓库
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataQualityResultRepository {

    /**
     * 批量保存结果
     */
    List<MetadataQualityResult> saveBatch(List<MetadataQualityResult> results);

    /**
     * 查询运行结果
     */
    List<MetadataQualityResult> listByRunId(Long runId);
}
