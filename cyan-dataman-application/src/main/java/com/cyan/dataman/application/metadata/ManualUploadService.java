package com.cyan.dataman.application.metadata;

import com.cyan.arch.common.api.Page;
import com.cyan.dataman.domain.metadata.ManualUploadRecord;
import org.springframework.web.multipart.MultipartFile;

/**
 * 手动上传服务接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface ManualUploadService {

    /**
     * 上传文件并导入数据
     *
     * @param tableId       元数据表ID
     * @param file          上传的文件
     * @param uploadMode    上传模式: overwrite/append
     * @param uploader      上传人passport
     * @param uploaderName  上传人姓名
     * @return 上传记录
     */
    ManualUploadRecord upload(Long tableId, MultipartFile file, String uploadMode, String uploader, String uploaderName);

    /**
     * 分页查询上传记录
     *
     * @param tableId   元数据表ID
     * @param pageNum   页码
     * @param pageSize  页大小
     * @return 上传记录分页结果
     */
    Page<ManualUploadRecord> listRecords(Long tableId, long pageNum, long pageSize);
}
