package com.cyan.dataman.domain.metadata;

import com.cyan.dataman.domain.metadata.repository.ManualUploadRecordRepository;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 手动上传记录领域对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class ManualUploadRecord {

    /**
     * 主键
     */
    private Long id;

    /**
     * 元数据表ID
     */
    private Long tableId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件类型: excel/csv
     */
    private String fileType;

    /**
     * 上传模式: overwrite/append
     */
    private String uploadMode;

    /**
     * 导入行数
     */
    private Integer rowCount;

    /**
     * 上传人passport
     */
    private String uploader;

    /**
     * 上传人姓名
     */
    private String uploaderName;

    /**
     * 状态: success/failed
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 保存上传记录
     */
    public ManualUploadRecord save(ManualUploadRecordRepository repository) {
        return repository.save(this);
    }
}
