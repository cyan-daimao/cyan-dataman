package com.cyan.dataman.domain.metadata.valobj;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "目录不能为空")
    private String catalog;

    /**
     * 库名
     */
    @NotBlank(message = "库名不能为空")
    private String schema;

    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String name;

    /**
     * 表注释
     */
    @NotBlank(message = "表注释不能为空")
    private String comment;

    /**
     * 字段列表
     */
    @NotEmpty(message = "字段列表不能为空")
    private List<ColumnValObj> columns;

    /**
     * 索引列表
     */
    private List<IndexValObj> indexes;
}
