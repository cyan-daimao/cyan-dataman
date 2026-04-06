package com.cyan.dataman.infra.persistence.cdc.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.dataman.domain.cdc.CdcSparkTask;
import com.cyan.dataman.domain.cdc.repository.CdcSparkTaskRepository;
import com.cyan.dataman.enums.JobStatus;
import com.cyan.dataman.infra.persistence.cdc.convert.CdcSparkTaskInfraConvert;
import com.cyan.dataman.infra.persistence.cdc.dos.CdcSparkTaskDO;
import com.cyan.dataman.infra.persistence.cdc.mappers.CdcSparkTaskMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CDC Spark 任务实例仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class CdcSparkTaskRepositoryImpl implements CdcSparkTaskRepository {

    private final CdcSparkTaskMapper cdcSparkTaskMapper;

    public CdcSparkTaskRepositoryImpl(CdcSparkTaskMapper cdcSparkTaskMapper) {
        this.cdcSparkTaskMapper = cdcSparkTaskMapper;
    }

    @Override
    public CdcSparkTask save(CdcSparkTask task) {
        CdcSparkTaskDO dos = CdcSparkTaskInfraConvert.INSTANCE.toDO(task);
        if (dos.getId() == null) {
            cdcSparkTaskMapper.insert(dos);
        } else {
            cdcSparkTaskMapper.updateById(dos);
        }
        return CdcSparkTaskInfraConvert.INSTANCE.toDomain(dos);
    }

    @Override
    public CdcSparkTask findById(String id) {
        CdcSparkTaskDO dos = cdcSparkTaskMapper.selectById(id);
        return dos != null ? CdcSparkTaskInfraConvert.INSTANCE.toDomain(dos) : null;
    }

    @Override
    public List<CdcSparkTask> findBySparkJobId(String sparkJobId) {
        LambdaQueryWrapper<CdcSparkTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcSparkTaskDO::getSparkJobId, sparkJobId)
                .orderByDesc(CdcSparkTaskDO::getCreatedAt);
        List<CdcSparkTaskDO> dosList = cdcSparkTaskMapper.selectList(wrapper);
        return dosList.stream()
                .map(CdcSparkTaskInfraConvert.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public List<CdcSparkTask> findByCdcConfigId(String cdcConfigId) {
        LambdaQueryWrapper<CdcSparkTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcSparkTaskDO::getCdcConfigId, cdcConfigId)
                .orderByDesc(CdcSparkTaskDO::getCreatedAt);
        List<CdcSparkTaskDO> dosList = cdcSparkTaskMapper.selectList(wrapper);
        return dosList.stream()
                .map(CdcSparkTaskInfraConvert.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public List<CdcSparkTask> findRunningTasks(LocalDateTime startTime) {
        LambdaQueryWrapper<CdcSparkTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcSparkTaskDO::getStatus, JobStatus.RUNNING)
                .gt(CdcSparkTaskDO::getStartTime, startTime);
        List<CdcSparkTaskDO> dosList = cdcSparkTaskMapper.selectList(wrapper);
        return dosList.stream()
                .map(CdcSparkTaskInfraConvert.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public List<CdcSparkTask> findByStatus(JobStatus status) {
        LambdaQueryWrapper<CdcSparkTaskDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CdcSparkTaskDO::getStatus, status)
                .orderByDesc(CdcSparkTaskDO::getCreatedAt);
        List<CdcSparkTaskDO> dosList = cdcSparkTaskMapper.selectList(wrapper);
        return dosList.stream()
                .map(CdcSparkTaskInfraConvert.INSTANCE::toDomain)
                .toList();
    }
}
