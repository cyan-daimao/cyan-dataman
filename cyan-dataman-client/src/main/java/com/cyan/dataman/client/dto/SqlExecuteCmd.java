package com.cyan.dataman.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * SQL执行命令
 *
 * @author cy.Y
 * @since 1.1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class SqlExecuteCmd {

    /**
     * SQL语句
     */
    private String sql;

    /**
     * 最大返回条数（仅对DQL查询有效）
     */
    private Integer limit;
}
