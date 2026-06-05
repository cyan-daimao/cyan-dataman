package com.cyan.dataman.domain.metadata.quality.repository;

import com.cyan.dataman.domain.metadata.quality.MetadataQualityRule;

import java.util.List;

/**
 * 元数据质量规则仓库
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataQualityRuleRepository {

    /**
     * 保存规则
     */
    MetadataQualityRule save(MetadataQualityRule rule);

    /**
     * 更新规则
     */
    MetadataQualityRule update(MetadataQualityRule rule);

    /**
     * 删除规则
     */
    void deleteById(Long id);

    /**
     * 根据ID查询规则
     */
    MetadataQualityRule findById(Long id);

    /**
     * 查询表规则
     */
    List<MetadataQualityRule> listByTableId(String tableId);

    /**
     * 查询表启用规则
     */
    List<MetadataQualityRule> listEnabledByTableId(String tableId);
}
