package com.cyan.dataman.adapter.http.bigdata.table.dto;

import com.cyan.dataman.enums.WriteMode;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 快照数据
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableSnapshotsDTO {
    /**
     * 快照id
     */
    private String id;

    /**
     * 写入模式
     */
    private WriteMode writeMode;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 总行数
     */
    private long total;
}
