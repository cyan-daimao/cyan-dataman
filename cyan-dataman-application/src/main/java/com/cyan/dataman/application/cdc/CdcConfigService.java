package com.cyan.dataman.application.cdc;

import com.cyan.dataman.application.cdc.bo.CdcConfigBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkJobBO;
import com.cyan.dataman.application.cdc.bo.CdcSparkTaskBO;
import com.cyan.dataman.application.cdc.cmd.CdcConfigCmd;
import com.cyan.dataman.application.cdc.cmd.CdcSparkJobCmd;
import com.cyan.dataman.domain.cdc.query.CdcConfigListQuery;

import java.util.List;

/**
 * CDC 配置服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface CdcConfigService {

    // ==================== CDC 配置管理 ====================

    /**
     * 创建 CDC 配置
     */
    CdcConfigBO create(CdcConfigCmd cmd);

    /**
     * 获取 CDC 配置列表
     */
    List<CdcConfigBO> list(CdcConfigListQuery query);

    /**
     * 根据 ID 获取 CDC 配置
     */
    CdcConfigBO findById(String id);

    /**
     * 根据名称获取 CDC 配置
     */
    CdcConfigBO findByName(String name);

    /**
     * 更新 CDC 配置
     */
    CdcConfigBO update(String id, CdcConfigCmd cmd);

    /**
     * 删除 CDC 配置
     */
    void delete(String id);

    /**
     * 开启/关闭 CDC
     */
    void toggle(String id, Boolean enabled);

    // ==================== Spark 作业配置管理 ====================

    /**
     * 创建 Spark 作业配置
     */
    CdcSparkJobBO createSparkJob(CdcSparkJobCmd cmd);

    /**
     * 获取 Spark 作业配置列表
     */
    List<CdcSparkJobBO> getSparkJobsByCdcConfigId(String cdcConfigId);

    /**
     * 更新 Spark 作业配置
     */
    CdcSparkJobBO updateSparkJob(String id, CdcSparkJobCmd cmd);

    /**
     * 删除 Spark 作业配置
     */
    void deleteSparkJob(String id);

    // ==================== Spark 任务实例管理 ====================

    /**
     * 获取任务实例列表
     */
    List<CdcSparkTaskBO> getTaskInstances(String cdcConfigId);

    /**
     * 获取任务实例详情
     */
    CdcSparkTaskBO getTaskInstance(String taskId);

    /**
     * 停止运行中的任务
     */
    void stopTask(String taskId);
}
