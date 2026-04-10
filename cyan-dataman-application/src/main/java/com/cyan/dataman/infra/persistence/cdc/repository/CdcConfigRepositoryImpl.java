package com.cyan.dataman.infra.persistence.cdc.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.infra.persistence.cdc.convert.CdcConfigInfraConvert;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcConfigDO;
import com.cyan.dataman.infra.persistence.cdc.mappers.CdcConfigMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CDC 配置仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class CdcConfigRepositoryImpl implements CdcConfigRepository {

    private final CdcConfigMapper cdcConfigMapper;

    public CdcConfigRepositoryImpl(CdcConfigMapper cdcConfigMapper) {
        this.cdcConfigMapper = cdcConfigMapper;
    }

    @Override
    public CdcConfig save(CdcConfig config) {
        CdcConfigDO dos = CdcConfigInfraConvert.INSTANCE.toDO(config);
        cdcConfigMapper.insert(dos);
        return CdcConfigInfraConvert.INSTANCE.toDomain(dos);
    }

    @Override
    public CdcConfig update(CdcConfig config) {
        CdcConfigDO dos = CdcConfigInfraConvert.INSTANCE.toDO(config);
        cdcConfigMapper.updateById(dos);
        return CdcConfigInfraConvert.INSTANCE.toDomain(dos);
    }

    @Override
    public CdcConfig findById(String id) {
        CdcConfigDO dos = cdcConfigMapper.selectById(id);
        return dos != null ? CdcConfigInfraConvert.INSTANCE.toDomain(dos) : null;
    }

    @Override
    public CdcConfig findByName(String name) {
        LambdaQueryWrapper<CdcConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcConfigDO::getName, name);
        CdcConfigDO dos = cdcConfigMapper.selectOne(wrapper);
        return dos != null ? CdcConfigInfraConvert.INSTANCE.toDomain(dos) : null;
    }

    @Override
    public List<CdcConfig> list(CdcConfigListQuery query) {
        LambdaQueryWrapper<CdcConfigDO> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(query.getDsId() != null, CdcConfigDO::getDsId, query.getDsId())
                    .eq(query.getDbName() != null, CdcConfigDO::getDbName, query.getDbName())
                    .eq(query.getTableName() != null, CdcConfigDO::getTableName, query.getTableName())
                    .eq(query.getEnabled() != null, CdcConfigDO::getEnabled, query.getEnabled());
        }
        List<CdcConfigDO> dosList = cdcConfigMapper.selectList(wrapper);
        return dosList.stream()
                .map(CdcConfigInfraConvert.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        cdcConfigMapper.deleteById(id);
    }

    @Override
    public List<CdcConfig> findByDatasource(String dsId) {
        List<CdcConfigDO> dosList = cdcConfigMapper.findByDatasource(dsId);
        return dosList.stream()
                .map(CdcConfigInfraConvert.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public List<CdcConfig> findEnabledByDatasource(String dsId) {
        List<CdcConfigDO> dosList = cdcConfigMapper.findEnabledByDatasource(dsId);
        return dosList.stream()
                .map(CdcConfigInfraConvert.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public int findNextServerId() {
        return cdcConfigMapper.findNextServerId();
    }
}
