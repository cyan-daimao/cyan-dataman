package com.cyan.dataman.infra.persistence.metadata.quality.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityResult;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityResultRepository;
import com.cyan.dataman.infra.persistence.metadata.quality.convert.MetadataQualityInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityResultDO;
import com.cyan.dataman.infra.persistence.metadata.quality.mappers.MetadataQualityResultMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 元数据质量结果仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class MetadataQualityResultRepositoryImpl implements MetadataQualityResultRepository {

    private final MetadataQualityResultMapper metadataQualityResultMapper;

    public MetadataQualityResultRepositoryImpl(MetadataQualityResultMapper metadataQualityResultMapper) {
        this.metadataQualityResultMapper = metadataQualityResultMapper;
    }

    /**
     * 批量保存结果
     */
    @Override
    public List<MetadataQualityResult> saveBatch(List<MetadataQualityResult> results) {
        List<MetadataQualityResult> savedResults = new ArrayList<>();
        for (MetadataQualityResult result : Optional.ofNullable(results).orElse(List.of())) {
            result.validate();
            MetadataQualityResultDO resultDO = MetadataQualityInfraConvert.INSTANCE.toResultDO(result);
            metadataQualityResultMapper.insert(resultDO);
            savedResults.add(MetadataQualityInfraConvert.INSTANCE.toResult(resultDO));
        }
        return savedResults;
    }

    /**
     * 查询运行结果
     */
    @Override
    public List<MetadataQualityResult> listByRunId(Long runId) {
        LambdaQueryWrapper<MetadataQualityResultDO> wrapper = new LambdaQueryWrapper<MetadataQualityResultDO>()
                .eq(MetadataQualityResultDO::getRunId, runId)
                .orderByAsc(MetadataQualityResultDO::getId);
        List<MetadataQualityResultDO> resultDOList = Optional.ofNullable(metadataQualityResultMapper.selectList(wrapper)).orElse(List.of());
        return MetadataQualityInfraConvert.INSTANCE.toResultList(resultDOList);
    }
}
