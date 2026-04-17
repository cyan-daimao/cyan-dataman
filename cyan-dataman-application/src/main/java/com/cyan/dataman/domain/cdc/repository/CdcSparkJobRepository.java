package com.cyan.dataman.domain.cdc.repository;

import com.cyan.dataman.domain.cdc.CdcSparkJob;

import java.util.List;

/**
 * CDC Spark 作业配置仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface CdcSparkJobRepository {

    /**
     * 保存
     */
    CdcSparkJob save(CdcSparkJob job);

    /**
     * 更新
     */
    CdcSparkJob update(CdcSparkJob job);

    /**
     * 根据 ID 查找
     */
    CdcSparkJob findById(String id);

    /**
     * 根据 CDC 配置 ID 查找列表
     */
    List<CdcSparkJob> findByCdcConfigId(String cdcConfigId);

    /**
     * 删除
     */
    void deleteById(String id);

    /**
     * 查找所有启用的 Spark Job
     */
    List<CdcSparkJob> findAllEnabled();
}
