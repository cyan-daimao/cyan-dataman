package com.cyan.dataman.domain.bigdata.table.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 表删除命令
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TableMetaDeleteCmd {
    /**
     * 目录
     */
    private String catalog;
    /**
     * 库名
     */
    private String db;
    /**
     * 表名
     */
    private String tbl;
}
