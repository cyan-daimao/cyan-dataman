package com.cyan.dataman.adapter.metadata.http.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 手动上传记录 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
public class ManualUploadRecordDTO {

    /**
     * 主键
     */
    private Long id;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
