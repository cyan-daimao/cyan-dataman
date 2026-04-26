package com.cyan.dataman.adapter.metadata.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.dto.ManualUploadRecordDTO;
import com.cyan.dataman.application.metadata.ManualUploadService;
import com.cyan.dataman.domain.metadata.ManualUploadRecord;
import com.cyan.employee.login.filter.UserContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 手动上传控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata/tables/{tableId}/upload")
public class ManualUploadController {

    private final ManualUploadService manualUploadService;

    /**
     * 构造器注入
     */
    public ManualUploadController(ManualUploadService manualUploadService) {
        this.manualUploadService = manualUploadService;
    }

    /**
     * 上传文件并导入数据
     *
     * @param tableId    元数据表ID
     * @param file       上传的文件
     * @param uploadMode 上传模式: overwrite/append
     * @return 上传记录
     */
    @PostMapping
    public Response<ManualUploadRecordDTO> upload(
            @PathVariable("tableId") Long tableId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadMode") String uploadMode) {
        String userId = UserContextHolder.getCurrentEmployee().getPassport();
        String userName = UserContextHolder.getCurrentEmployee().getCnName();
        ManualUploadRecord record = manualUploadService.upload(tableId, file, uploadMode, userId, userName);
        return Response.success(toDTO(record));
    }

    /**
     * 分页查询上传记录
     *
     * @param tableId   元数据表ID
     * @param pageNum   页码
     * @param pageSize  页大小
     * @return 上传记录分页列表
     */
    @GetMapping("/records")
    public Response<Page<ManualUploadRecordDTO>> listRecords(
            @PathVariable("tableId") Long tableId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<ManualUploadRecord> page = manualUploadService.listRecords(tableId, pageNum, pageSize);
        return Response.success(new Page<>(
                page.getData().stream().map(this::toDTO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal()
        ));
    }

    /**
     * 领域对象转 DTO
     *
     * @param record 上传记录领域对象
     * @return 上传记录 DTO
     */
    private ManualUploadRecordDTO toDTO(ManualUploadRecord record) {
        ManualUploadRecordDTO dto = new ManualUploadRecordDTO();
        dto.setId(record.getId());
        dto.setFileName(record.getFileName());
        dto.setFileType(record.getFileType());
        dto.setUploadMode(record.getUploadMode());
        dto.setRowCount(record.getRowCount());
        dto.setUploaderName(record.getUploaderName());
        dto.setStatus(record.getStatus());
        dto.setErrorMessage(record.getErrorMessage());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }
}
