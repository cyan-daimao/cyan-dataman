package com.cyan.dataman.domain.bigdata.table.query;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 表数据分页查询参数
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableDataListQuery {

    @NotBlank(message = "目录不能为空")
    private String catalog;

    /**
     * 库
     */
    @NotBlank(message = "库不能为空")
    private String db;
    /**
     * 表
     */
    @NotBlank(message = "表不能为空")
    private String name;

    /**
     * 取n个数据
     */
    private int limit;


    /**
     * 获取完整表名
     */
    public String fullName() {
        return catalog + "." + db + "." + name;
    }
}
