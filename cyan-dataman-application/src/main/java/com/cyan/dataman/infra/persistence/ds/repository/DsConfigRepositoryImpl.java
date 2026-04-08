package com.cyan.dataman.infra.persistence.ds.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.ds.DsConfig;
import com.cyan.dataman.domain.ds.query.DsConfigFindQuery;
import com.cyan.dataman.domain.ds.query.DsConfigListQuery;
import com.cyan.dataman.domain.ds.repository.DsConfigRepository;
import com.cyan.dataman.infra.persistence.ds.convert.DsConfigInfraConvert;
import com.cyan.dataman.infra.persistence.ds.dos.DsConfigDO;
import com.cyan.dataman.infra.persistence.ds.mappers.DsConfigMapper;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据源配置仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class DsConfigRepositoryImpl implements DsConfigRepository {
    private final DsConfigMapper dsConfigMapper;

    public DsConfigRepositoryImpl(DsConfigMapper dsConfigMapper) {
        this.dsConfigMapper = dsConfigMapper;
    }

    @Override
    public DsConfig findById(String id) {
        DsConfigDO dsConfigDO = dsConfigMapper.selectById(id);
        return DsConfigInfraConvert.INSTANCE.toDsConfig(dsConfigDO);
    }

    @Override
    public List<DsConfig> list(DsConfigListQuery query) {
        query = query == null ? new DsConfigListQuery() : query;
        LambdaQueryWrapper<DsConfigDO> queryWrapper = new LambdaQueryWrapper<DsConfigDO>()
                .like(StringUtils.isNotBlank(query.getName()), DsConfigDO::getName, query.getName())
                .eq(query.getDatasourceType() != null, DsConfigDO::getDatasourceType, query.getDatasourceType());
        List<DsConfigDO> dsConfigDOS = dsConfigMapper.selectList(queryWrapper);
        return Optional.ofNullable(dsConfigDOS).orElse(List.of()).stream()
                .map(DsConfigInfraConvert.INSTANCE::toDsConfig)
                .toList();
    }

    @Override
    public DsConfig find(DsConfigFindQuery query) {
        LambdaQueryWrapper<DsConfigDO> queryWrapper = new LambdaQueryWrapper<DsConfigDO>()
                .eq(StringUtils.isNotBlank(query.getName()), DsConfigDO::getName, query.getName());
        DsConfigDO dsConfigDO = dsConfigMapper.selectOne(queryWrapper);
        return DsConfigInfraConvert.INSTANCE.toDsConfig(dsConfigDO);
    }

    @Override
    public DsConfig save(DsConfig dsConfig) {
        DsConfigDO dsConfigDO = DsConfigInfraConvert.INSTANCE.toDsConfigDO(dsConfig);
        dsConfigMapper.insert(dsConfigDO);
        return findById(dsConfigDO.getId());
    }

    @Override
    public DsConfig update(DsConfig dsConfig) {
        DsConfigDO dsConfigDO = DsConfigInfraConvert.INSTANCE.toDsConfigDO(dsConfig);
        dsConfigMapper.updateById(dsConfigDO);
        return findById(dsConfigDO.getId());
    }

    @Override
    public void deleteById(String id) {
        dsConfigMapper.deleteById(id);
    }

    /**
     * 根据名称获取数据源配置
     *
     */
    @Override
    public DsConfig findByName(String ds) {
        LambdaQueryWrapper<DsConfigDO> queryWrapper = new LambdaQueryWrapper<DsConfigDO>()
                .eq(DsConfigDO::getName, ds);
        DsConfigDO dsConfigDO = dsConfigMapper.selectOne(queryWrapper);
        return DsConfigInfraConvert.INSTANCE.toDsConfig(dsConfigDO);
    }
}
