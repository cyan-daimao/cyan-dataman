package com.cyan.dataman.domain.bigdata.table.query;

import com.cyan.arch.common.api.Pagination;
import com.cyan.dataman.enums.DataLayer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 表查询参数
 *
 * @author cy.Y
 * @since v1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Data
public class TableQuery extends Pagination {

    /**
     * 数据库
     */
    private DataLayer db;

    /**
     * 表名
     */
    private String name;

}
