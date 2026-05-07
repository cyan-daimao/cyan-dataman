package com.cyan.dataman.client.table.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 表关系传输对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableRelationDTO {

    /**
     * 主键
     */
    private String id;

    /**
     * 源表 catalog
     */
    private String sourceCatalog;

    /**
     * 源表 schema
     */
    private String sourceSchema;

    /**
     * 源表名
     */
    private String sourceTable;

    /**
     * 源表字段
     */
    private String sourceColumn;

    /**
     * 目标表 catalog
     */
    private String targetCatalog;

    /**
     * 目标表 schema
     */
    private String targetSchema;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 目标表字段
     */
    private String targetColumn;

    /**
     * JOIN类型：LEFT/INNER/RIGHT
     */
    private String joinType;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
