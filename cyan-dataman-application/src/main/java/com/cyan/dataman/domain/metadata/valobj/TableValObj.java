package com.cyan.dataman.domain.metadata.valobj;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 表值对象
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableValObj {

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
    private List<ColumnValObj> columns;

    /**
     * 索引列表
     */
    private List<IndexValObj> indexes;
}
