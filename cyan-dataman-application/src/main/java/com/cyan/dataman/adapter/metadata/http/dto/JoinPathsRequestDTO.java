package com.cyan.dataman.adapter.metadata.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 批量查询 JOIN 路径请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class JoinPathsRequestDTO {

    /**
     * 事实表
     */
    private TableRefDTO factTable;

    /**
     * 维度表列表
     */
    private List<TableRefDTO> dimensionTables;

    /**
     * 表引用
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Accessors(chain = true)
    public static class TableRefDTO {

        /**
         * catalog
         */
        private String catalog;

        /**
         * schema
         */
        private String schema;

        /**
         * 表名
         */
        private String table;
    }
}
