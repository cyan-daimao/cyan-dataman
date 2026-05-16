package com.cyan.dataman.infra.persistence.metadata.dos;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 手动上传记录表 DO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@TableName("manual_upload_record")
public class ManualUploadRecordDO {

    /**
     * 主键
     */
    @TableId(value = "id",type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 元数据表ID
     */
    @TableField(value = "table_id")
    private Long tableId;

    /**
     * 原始文件名
     */
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 文件类型: excel/csv
     */
    @TableField(value = "file_type")
    private String fileType;

    /**
     * 上传模式: overwrite/append
     */
    @TableField(value = "upload_mode")
    private String uploadMode;

    /**
     * 导入行数
     */
    @TableField(value = "row_count")
    private Integer rowCount;

    /**
     * 上传人passport
     */
    @TableField(value = "uploader")
    private String uploader;

    /**
     * 上传人姓名
     */
    @TableField(value = "uploader_name")
    private String uploaderName;

    /**
     * 状态: success/failed
     */
    @TableField(value = "status")
    private String status;

    /**
     * 错误信息
     */
    @TableField(value = "error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除时间
     */
    @TableField(value = "deleted_at")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}
