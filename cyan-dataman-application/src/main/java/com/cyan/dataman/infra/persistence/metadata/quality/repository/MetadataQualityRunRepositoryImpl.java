package com.cyan.dataman.infra.persistence.metadata.quality.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityRun;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityRunRepository;
import com.cyan.dataman.infra.persistence.metadata.quality.convert.MetadataQualityInfraConvert;
import com.cyan.dataman.infra.persistence.metadata.quality.dos.MetadataQualityRunDO;
import com.cyan.dataman.infra.persistence.metadata.quality.mappers.MetadataQualityRunMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 元数据质量运行仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class MetadataQualityRunRepositoryImpl implements MetadataQualityRunRepository {

    private final MetadataQualityRunMapper metadataQualityRunMapper;

    public MetadataQualityRunRepositoryImpl(MetadataQualityRunMapper metadataQualityRunMapper) {
        this.metadataQualityRunMapper = metadataQualityRunMapper;
    }

    /**
     * 保存运行记录
     */
    @Override
    public MetadataQualityRun save(MetadataQualityRun run) {
        MetadataQualityRunDO runDO = MetadataQualityInfraConvert.INSTANCE.toRunDO(run);
        metadataQualityRunMapper.insert(runDO);
        return findById(runDO.getId());
    }

    /**
     * 更新运行记录
     */
    @Override
    public MetadataQualityRun update(MetadataQualityRun run) {
        MetadataQualityRunDO runDO = MetadataQualityInfraConvert.INSTANCE.toRunDO(run);
        metadataQualityRunMapper.updateById(runDO);
        return findById(runDO.getId());
    }

    /**
     * 根据ID查询运行记录
     */
    @Override
    public MetadataQualityRun findById(Long id) {
        return MetadataQualityInfraConvert.INSTANCE.toRun(metadataQualityRunMapper.selectById(id));
    }

    /**
     * 查询表运行记录
     */
    @Override
    public List<MetadataQualityRun> listByTableId(String tableId, Integer limit) {
        LambdaQueryWrapper<MetadataQualityRunDO> wrapper = new LambdaQueryWrapper<MetadataQualityRunDO>()
                .eq(MetadataQualityRunDO::getTableId, tableId)
                .orderByDesc(MetadataQualityRunDO::getStartedAt)
                .last("limit " + (limit == null ? 20 : limit));
        List<MetadataQualityRunDO> runDOList = Optional.ofNullable(metadataQualityRunMapper.selectList(wrapper)).orElse(List.of());
        return MetadataQualityInfraConvert.INSTANCE.toRunList(runDOList);
    }

    /**
     * 查询表最近一次运行
     */
    @Override
    public MetadataQualityRun findLatestByTableId(String tableId) {
        LambdaQueryWrapper<MetadataQualityRunDO> wrapper = new LambdaQueryWrapper<MetadataQualityRunDO>()
                .eq(MetadataQualityRunDO::getTableId, tableId)
                .orderByDesc(MetadataQualityRunDO::getStartedAt)
                .last("limit 1");
        return MetadataQualityInfraConvert.INSTANCE.toRun(metadataQualityRunMapper.selectOne(wrapper));
    }
}
