package com.cyan.dataman.domain.metadata.valobj;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 快照
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableSnapshotValObj {
    /**
     * 快照id
     */
    private String snapshotId;

    /**
     * 操作: append , replace
     */
    private String operation;

    /**
     * 序列号
     */
    private String sequenceNumber;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 快照元数据文件
     */
    private String manifestListLocation;

    /**
     * 当前快照总记录数
     */
    private String totalRecords;

    /**
     * 当前快照新增记录数
     */
    private String addedRecords;
}
