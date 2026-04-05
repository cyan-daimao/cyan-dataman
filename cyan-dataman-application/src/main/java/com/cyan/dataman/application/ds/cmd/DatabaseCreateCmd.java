package com.cyan.dataman.application.ds.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据库创建命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DatabaseCreateCmd {

    /**
     * 数据库名
     */
    private String name;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;
}
