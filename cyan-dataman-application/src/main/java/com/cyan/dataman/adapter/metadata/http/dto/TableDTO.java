package com.cyan.dataman.adapter.metadata.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 表对象
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableDTO {
    /**
     * 目录
     */
    private String catalog;

    /**
     * 库名
     */
    private String schema;

    /**
     * 表名
     */
    private String name;

    /**
     * 表注释
     */
    private String comment;

    /**
     * 字段列表
     */
    private List<ColumnDTO> columns;

    /**
     * 索引列表
     */
    private List<IndexDTO> indexes;
}
