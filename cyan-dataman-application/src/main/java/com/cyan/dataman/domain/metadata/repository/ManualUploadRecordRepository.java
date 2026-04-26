package com.cyan.dataman.domain.metadata.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.dataman.domain.metadata.ManualUploadRecord;

/**
 * 手动上传记录仓储接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface ManualUploadRecordRepository {

    /**
     * 保存上传记录
     *
     * @param record 上传记录领域对象
     * @return 保存后的上传记录
     */
    ManualUploadRecord save(ManualUploadRecord record);

    /**
     * 根据元数据表ID分页查询上传记录
     *
     * @param tableId  元数据表ID
     * @param pageNum  页码
     * @param pageSize 页大小
     * @return 上传记录分页结果
     */
    Page<ManualUploadRecord> pageByTableId(Long tableId, long pageNum, long pageSize);
}
