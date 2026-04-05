package com.cyan.dataman.adapter.ds.http.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据库DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DatabaseDTO {

    /**
     * 数据库名
     */
    private String name;

    /**
     * 数据库注释
     */
    private String comment;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;
}
