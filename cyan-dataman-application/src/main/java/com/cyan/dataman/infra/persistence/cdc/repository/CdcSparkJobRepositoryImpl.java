package com.cyan.dataman.infra.persistence.cdc.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.domain.cdc.repository.CdcSparkJobRepository;
import com.cyan.dataman.infra.persistence.cdc.convert.CdcSparkJobInfraConvert;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcSparkJobDO;
import com.cyan.dataman.infra.persistence.cdc.mappers.CdcSparkJobMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CDC Spark 作业配置仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class CdcSparkJobRepositoryImpl implements CdcSparkJobRepository {

    private final CdcSparkJobMapper cdcSparkJobMapper;

    public CdcSparkJobRepositoryImpl(CdcSparkJobMapper cdcSparkJobMapper) {
        this.cdcSparkJobMapper = cdcSparkJobMapper;
    }

    @Override
    public CdcSparkJob save(CdcSparkJob job) {
        CdcSparkJobDO dos = CdcSparkJobInfraConvert.INSTANCE.toDO(job);
        cdcSparkJobMapper.insert(dos);
        return CdcSparkJobInfraConvert.INSTANCE.toDomain(dos);
    }

    @Override
    public CdcSparkJob update(CdcSparkJob job) {
        CdcSparkJobDO dos = CdcSparkJobInfraConvert.INSTANCE.toDO(job);
        cdcSparkJobMapper.updateById(dos);
        return CdcSparkJobInfraConvert.INSTANCE.toDomain(dos);
    }

    @Override
    public CdcSparkJob findById(String id) {
        CdcSparkJobDO dos = cdcSparkJobMapper.selectById(id);
        return dos != null ? CdcSparkJobInfraConvert.INSTANCE.toDomain(dos) : null;
    }

    @Override
    public List<CdcSparkJob> findByCdcConfigId(String cdcConfigId) {
        LambdaQueryWrapper<CdcSparkJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcSparkJobDO::getCdcConfigId, cdcConfigId);
        List<CdcSparkJobDO> dosList = cdcSparkJobMapper.selectList(wrapper);
        return dosList.stream()
                .map(CdcSparkJobInfraConvert.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        cdcSparkJobMapper.deleteById(id);
    }
}
