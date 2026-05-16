package com.cyan.dataman.infra.persistence.cdc.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.cdc.CdcFlinkJob;
import com.cyan.dataman.domain.cdc.repository.CdcFlinkJobRepository;
import com.cyan.dataman.infra.persistence.cdc.convert.CdcFlinkJobInfraConvert;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcFlinkJobDO;
import com.cyan.dataman.infra.persistence.cdc.mappers.CdcFlinkJobMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CDC Flink 作业配置仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class CdcFlinkJobRepositoryImpl implements CdcFlinkJobRepository {

    private final CdcFlinkJobMapper cdcFlinkJobMapper;

    public CdcFlinkJobRepositoryImpl(CdcFlinkJobMapper cdcFlinkJobMapper) {
        this.cdcFlinkJobMapper = cdcFlinkJobMapper;
    }

    @Override
    public CdcFlinkJob save(CdcFlinkJob job) {
        CdcFlinkJobDO dos = CdcFlinkJobInfraConvert.INSTANCE.toDO(job);
        cdcFlinkJobMapper.insert(dos);
        return CdcFlinkJobInfraConvert.INSTANCE.toDomain(dos);
    }

    @Override
    public CdcFlinkJob update(CdcFlinkJob job) {
        CdcFlinkJobDO dos = CdcFlinkJobInfraConvert.INSTANCE.toDO(job);
        cdcFlinkJobMapper.updateById(dos);
        return CdcFlinkJobInfraConvert.INSTANCE.toDomain(dos);
    }

    @Override
    public CdcFlinkJob findById(Long id) {
        CdcFlinkJobDO dos = cdcFlinkJobMapper.selectById(id);
        return dos != null ? CdcFlinkJobInfraConvert.INSTANCE.toDomain(dos) : null;
    }

    @Override
    public CdcFlinkJob findByDsNameAndSubjectCode(String dsName, String subjectCode) {
        LambdaQueryWrapper<CdcFlinkJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcFlinkJobDO::getDsName, dsName)
               .eq(CdcFlinkJobDO::getSubjectCode, subjectCode);
        CdcFlinkJobDO dos = cdcFlinkJobMapper.selectOne(wrapper);
        return dos != null ? CdcFlinkJobInfraConvert.INSTANCE.toDomain(dos) : null;
    }

    @Override
    public CdcFlinkJob findByFlinkJobId(String flinkJobId) {
        LambdaQueryWrapper<CdcFlinkJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcFlinkJobDO::getFlinkJobId, flinkJobId);
        CdcFlinkJobDO dos = cdcFlinkJobMapper.selectOne(wrapper);
        return dos != null ? CdcFlinkJobInfraConvert.INSTANCE.toDomain(dos) : null;
    }

    @Override
    public List<CdcFlinkJob> findAllRunning() {
        LambdaQueryWrapper<CdcFlinkJobDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcFlinkJobDO::getStatus, com.cyan.dataman.enums.JobStatus.RUNNING);
        List<CdcFlinkJobDO> dosList = cdcFlinkJobMapper.selectList(wrapper);
        return dosList.stream()
                .map(CdcFlinkJobInfraConvert.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        cdcFlinkJobMapper.deleteById(id);
    }
}
