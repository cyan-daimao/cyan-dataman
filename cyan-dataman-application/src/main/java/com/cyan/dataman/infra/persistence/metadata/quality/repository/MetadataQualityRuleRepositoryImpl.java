package com.cyan.dataman.infra.persistence.metadata.quality.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityRule;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityRuleRepository;
import com.cyan.dataman.infra.persistence.metadata.quality.convert.MetadataQualityInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityRuleDO;
import com.cyan.dataman.infra.persistence.metadata.quality.mappers.MetadataQualityRuleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 元数据质量规则仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class MetadataQualityRuleRepositoryImpl implements MetadataQualityRuleRepository {

    private final MetadataQualityRuleMapper metadataQualityRuleMapper;

    public MetadataQualityRuleRepositoryImpl(MetadataQualityRuleMapper metadataQualityRuleMapper) {
        this.metadataQualityRuleMapper = metadataQualityRuleMapper;
    }

    /**
     * 保存规则
     */
    @Override
    public MetadataQualityRule save(MetadataQualityRule rule) {
        MetadataQualityRuleDO ruleDO = MetadataQualityInfraConvert.INSTANCE.toRuleDO(rule);
        metadataQualityRuleMapper.insert(ruleDO);
        return findById(ruleDO.getId());
    }

    /**
     * 更新规则
     */
    @Override
    public MetadataQualityRule update(MetadataQualityRule rule) {
        MetadataQualityRuleDO ruleDO = MetadataQualityInfraConvert.INSTANCE.toRuleDO(rule);
        metadataQualityRuleMapper.updateById(ruleDO);
        return findById(ruleDO.getId());
    }

    /**
     * 删除规则
     */
    @Override
    public void deleteById(Long id) {
        metadataQualityRuleMapper.deleteById(id);
    }

    /**
     * 根据ID查询规则
     */
    @Override
    public MetadataQualityRule findById(Long id) {
        return MetadataQualityInfraConvert.INSTANCE.toRule(metadataQualityRuleMapper.selectById(id));
    }

    /**
     * 查询表规则
     */
    @Override
    public List<MetadataQualityRule> listByTableId(String tableId) {
        LambdaQueryWrapper<MetadataQualityRuleDO> wrapper = new LambdaQueryWrapper<MetadataQualityRuleDO>()
                .eq(MetadataQualityRuleDO::getTableId, tableId)
                .orderByDesc(MetadataQualityRuleDO::getCreatedAt);
        List<MetadataQualityRuleDO> ruleDOList = Optional.ofNullable(metadataQualityRuleMapper.selectList(wrapper)).orElse(List.of());
        return MetadataQualityInfraConvert.INSTANCE.toRuleList(ruleDOList);
    }

    /**
     * 查询表启用规则
     */
    @Override
    public List<MetadataQualityRule> listEnabledByTableId(String tableId) {
        LambdaQueryWrapper<MetadataQualityRuleDO> wrapper = new LambdaQueryWrapper<MetadataQualityRuleDO>()
                .eq(MetadataQualityRuleDO::getTableId, tableId)
                .eq(MetadataQualityRuleDO::getEnabled, true)
                .orderByAsc(MetadataQualityRuleDO::getId);
        List<MetadataQualityRuleDO> ruleDOList = Optional.ofNullable(metadataQualityRuleMapper.selectList(wrapper)).orElse(List.of());
        return MetadataQualityInfraConvert.INSTANCE.toRuleList(ruleDOList);
    }
}
