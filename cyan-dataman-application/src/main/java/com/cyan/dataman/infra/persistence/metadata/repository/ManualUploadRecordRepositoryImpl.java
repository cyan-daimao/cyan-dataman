package com.cyan.dataman.infra.persistence.metadata.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyan.dataman.domain.metadata.ManualUploadRecord;
import com.cyan.dataman.domain.metadata.repository.ManualUploadRecordRepository;
import com.cyan.dataman.infra.persistence.metadata.dos.ManualUploadRecordDO;
import com.cyan.dataman.infra.persistence.metadata.mappers.ManualUploadRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 手动上传记录仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
public class ManualUploadRecordRepositoryImpl implements ManualUploadRecordRepository {

    private final ManualUploadRecordMapper mapper;

    public ManualUploadRecordRepositoryImpl(ManualUploadRecordMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ManualUploadRecord save(ManualUploadRecord record) {
        ManualUploadRecordDO recordDO = new ManualUploadRecordDO();
        recordDO.setTableId(record.getTableId());
        recordDO.setFileName(record.getFileName());
        recordDO.setFileType(record.getFileType());
        recordDO.setUploadMode(record.getUploadMode());
        recordDO.setRowCount(record.getRowCount());
        recordDO.setUploader(record.getUploader());
        recordDO.setUploaderName(record.getUploaderName());
        recordDO.setStatus(record.getStatus());
        recordDO.setErrorMessage(record.getErrorMessage());
        recordDO.setUpdatedAt(record.getUpdatedAt());
        recordDO.setDeletedAt(record.getDeletedAt());
        mapper.insert(recordDO);
        record.setId(recordDO.getId());
        record.setCreatedAt(recordDO.getCreatedAt());
        record.setUpdatedAt(recordDO.getUpdatedAt());
        return record;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public com.cyan.arch.common.api.Page<ManualUploadRecord> pageByTableId(Long tableId, long pageNum, long pageSize) {
        Page<ManualUploadRecordDO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ManualUploadRecordDO> wrapper = new LambdaQueryWrapper<ManualUploadRecordDO>()
                .eq(ManualUploadRecordDO::getTableId, tableId)
                .orderByDesc(ManualUploadRecordDO::getCreatedAt);
        Page<ManualUploadRecordDO> result = mapper.selectPage(page, wrapper);
        java.util.List<ManualUploadRecord> list = Optional.ofNullable(result.getRecords()).orElse(java.util.List.of()).stream()
                .map(d -> {
                    ManualUploadRecord r = new ManualUploadRecord();
                    r.setId(d.getId());
                    r.setTableId(d.getTableId());
                    r.setFileName(d.getFileName());
                    r.setFileType(d.getFileType());
                    r.setUploadMode(d.getUploadMode());
                    r.setRowCount(d.getRowCount());
                    r.setUploader(d.getUploader());
                    r.setUploaderName(d.getUploaderName());
                    r.setStatus(d.getStatus());
                    r.setErrorMessage(d.getErrorMessage());
                    r.setCreatedAt(d.getCreatedAt());
                    r.setUpdatedAt(d.getUpdatedAt());
                    r.setDeletedAt(d.getDeletedAt());
                    return r;
                })
                .toList();
        return new com.cyan.arch.common.api.Page<>(list, result.getCurrent(), result.getSize(), result.getTotal());
    }
}
