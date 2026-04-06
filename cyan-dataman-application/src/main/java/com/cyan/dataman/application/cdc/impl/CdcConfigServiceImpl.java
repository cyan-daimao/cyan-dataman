package com.cyan.dataman.application.cdc.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.cdc.CdcConfigService;
import com.cyan.dataman.application.cdc.bo.CdcConfigBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkJobBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkTaskBO;
import com.cyan.dataman.application.cdc.cmd.CdcConfigCmd;
import com.cyan.dataman.application.cdc.cmd.CdcSparkJobCmd;
import com.cyan.dataman.application.cdc.convert.CdcAppConvert;
import com.cyan.dataman.domain.cdc.CdcConfig;
import com.cyan.dataman.domain.cdc.CdcSparkJob;
import com.cyan.dataman.domain.cdc.CdcSparkTask;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;
import com.cyan.dataman.domain.cdc.repository.CdcConfigRepository;
import com.cyan.dataman.domain.cdc.repository.CdcSparkJobRepository;
import com.cyan.dataman.domain.cdc.repository.CdcSparkTaskRepository;
import com.cyan.dataman.enums.JobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CDC 配置服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
public class CdcConfigServiceImpl implements CdcConfigService {

    private final CdcConfigRepository cdcConfigRepository;
    private final CdcSparkJobRepository cdcSparkJobRepository;
    private final CdcSparkTaskRepository cdcSparkTaskRepository;

    public CdcConfigServiceImpl(CdcConfigRepository cdcConfigRepository,
                                CdcSparkJobRepository cdcSparkJobRepository,
                                CdcSparkTaskRepository cdcSparkTaskRepository) {
        this.cdcConfigRepository = cdcConfigRepository;
        this.cdcSparkJobRepository = cdcSparkJobRepository;
        this.cdcSparkTaskRepository = cdcSparkTaskRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcConfigBO create(CdcConfigCmd cmd) {
        // 检查名称是否已存在
        CdcConfig existing = cdcConfigRepository.findByName(cmd.getName());
        Assert.isNull(existing, new SilentException("CDC 配置名称已存在"));

        CdcConfig config = CdcAppConvert.INSTANCE.toDomain(cmd);
        config = config.save(cdcConfigRepository);
        return CdcAppConvert.INSTANCE.toBO(config);
    }

    @Override
    public List<CdcConfigBO> list(CdcConfigListQuery query) {
        List<CdcConfig> list = cdcConfigRepository.list(query);
        return Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAppConvert.INSTANCE::toBO)
                .toList();
    }

    @Override
    public CdcConfigBO findById(String id) {
        CdcConfig config = cdcConfigRepository.findById(id);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));
        return CdcAppConvert.INSTANCE.toBO(config);
    }

    @Override
    public CdcConfigBO findByName(String name) {
        CdcConfig config = cdcConfigRepository.findByName(name);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));
        return CdcAppConvert.INSTANCE.toBO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcConfigBO update(String id, CdcConfigCmd cmd) {
        CdcConfig config = cdcConfigRepository.findById(id);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        // 检查名称是否被其他配置使用
        CdcConfig existing = cdcConfigRepository.findByName(cmd.getName());
        if (existing != null && !existing.getId().equals(id)) {
            throw new SilentException("CDC 配置名称已存在");
        }

        config.setName(cmd.getName())
                .setDsId(cmd.getDsId())
                .setDbName(cmd.getDbName())
                .setTableName(cmd.getTableName())
                .setIcebergTableName(cmd.getIcebergTableName())
                .setSyncTool(cmd.getSyncTool())
                .setSyncSql(cmd.getSyncSql())
                .setDescription(cmd.getDescription())
                .setUpdateBy(cmd.getUpdateBy());

        config = config.update(cdcConfigRepository);
        return CdcAppConvert.INSTANCE.toBO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        CdcConfig config = cdcConfigRepository.findById(id);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));
        config.delete(cdcConfigRepository);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggle(String id, Boolean enabled) {
        CdcConfig config = cdcConfigRepository.findById(id);
        Assert.notNull(config, new SilentException("CDC 配置不存在"));
        config.toggle(cdcConfigRepository, enabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcSparkJobBO createSparkJob(CdcSparkJobCmd cmd) {
        // 检查 CDC 配置是否存在
        CdcConfig config = cdcConfigRepository.findById(cmd.getCdcConfigId());
        Assert.notNull(config, new SilentException("CDC 配置不存在"));

        CdcSparkJob job = CdcAppConvert.INSTANCE.toDomain(cmd);
        job = job.save(cdcSparkJobRepository);
        return CdcAppConvert.INSTANCE.toBO(job);
    }

    @Override
    public List<CdcSparkJobBO> getSparkJobsByCdcConfigId(String cdcConfigId) {
        List<CdcSparkJob> list = cdcSparkJobRepository.findByCdcConfigId(cdcConfigId);
        return Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAppConvert.INSTANCE::toBO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdcSparkJobBO updateSparkJob(String id, CdcSparkJobCmd cmd) {
        CdcSparkJob job = cdcSparkJobRepository.findById(id);
        Assert.notNull(job, new SilentException("Spark 作业配置不存在"));

        job.setSyncMode(cmd.getSyncMode())
                .setSparkSql(cmd.getSparkSql())
                .setCronExpression(cmd.getCronExpression())
                .setEnabled(cmd.getEnabled())
                .setUpdateBy(cmd.getUpdateBy());

        job = job.update(cdcSparkJobRepository);
        return CdcAppConvert.INSTANCE.toBO(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSparkJob(String id) {
        CdcSparkJob job = cdcSparkJobRepository.findById(id);
        Assert.notNull(job, new SilentException("Spark 作业配置不存在"));
        job.delete(cdcSparkJobRepository);
    }

    @Override
    public List<CdcSparkTaskBO> getTaskInstances(String cdcConfigId) {
        List<CdcSparkTask> list = cdcSparkTaskRepository.findByCdcConfigId(cdcConfigId);
        return Optional.ofNullable(list).orElse(List.of()).stream()
                .map(CdcAppConvert.INSTANCE::toBO)
                .toList();
    }

    @Override
    public CdcSparkTaskBO getTaskInstance(String taskId) {
        CdcSparkTask task = cdcSparkTaskRepository.findById(taskId);
        Assert.notNull(task, new SilentException("任务实例不存在"));
        return CdcAppConvert.INSTANCE.toBO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopTask(String taskId) {
        CdcSparkTask task = cdcSparkTaskRepository.findById(taskId);
        Assert.notNull(task, new SilentException("任务实例不存在"));

        if (task.getStatus() == JobStatus.RUNNING) {
            task.stop(cdcSparkTaskRepository);
        }
    }
}
