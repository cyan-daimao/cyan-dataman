package com.cyan.dataman.domain.cdc.repository;

import com.cyan.dataman.domain.cdc.CdcFlinkJob;

import java.util.List;

/**
 * CDC Flink 作业配置仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface CdcFlinkJobRepository {

    /**
     * 保存
     */
    CdcFlinkJob save(CdcFlinkJob job);

    /**
     * 更新
     */
    CdcFlinkJob update(CdcFlinkJob job);

    /**
     * 根据 ID 查找
     */
    CdcFlinkJob findById(Long id);

    /**
     * 根据数据源名称 + 主题编码查找
     */
    CdcFlinkJob findByDsNameAndSubjectCode(String dsName, String subjectCode);

    /**
     * 根据 Flink Job ID 查找
     */
    CdcFlinkJob findByFlinkJobId(String flinkJobId);

    /**
     * 查询所有运行中的 Flink 作业
     */
    List<CdcFlinkJob> findAllRunning();

    /**
     * 删除
     */
    void deleteById(Long id);
}
