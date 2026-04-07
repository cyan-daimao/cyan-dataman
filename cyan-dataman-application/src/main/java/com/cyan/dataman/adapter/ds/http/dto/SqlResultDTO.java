package com.cyan.dataman.adapter.ds.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * SQL执行结果DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class SqlResultDTO {

    /**
     * 是否为查询语句
     */
    private Boolean isQuery;

    /**
     * 列名列表（仅查询语句有效）
     */
    private List<String> columns;

    /**
     * 数据行列表（仅查询语句有效）
     */
    private List<Map<String, Object>> rows;

    /**
     * 返回行数（仅查询语句有效）
     */
    private Integer rowCount;

    /**
     * 影响行数（仅DML语句有效）
     */
    private Integer affectedRows;

    /**
     * 创建查询结果
     */
    public static SqlResultDTO ofQuery(List<String> columns, List<Map<String, Object>> rows, Integer rowCount) {
        return new SqlResultDTO()
                .setIsQuery(true)
                .setColumns(columns)
                .setRows(rows)
                .setRowCount(rowCount)
                .setAffectedRows(null);
    }

    /**
     * 创建DML结果
     */
    public static SqlResultDTO ofDml(int affectedRows) {
        return new SqlResultDTO()
                .setIsQuery(false)
                .setColumns(null)
                .setRows(null)
                .setRowCount(null)
                .setAffectedRows(affectedRows);
    }
}
