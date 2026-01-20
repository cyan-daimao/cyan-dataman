package com.cyan.dataman.domain.bigdata.table.cmd;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 创建表命令
 *
 * @author cy.Y
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TableMetaCmd {

    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String tbl;

    /**
     * catalog
     */
    @NotNull(message = "catalog不能为空")
    private String catalog;

    /**
     * 数据库名
     */
    @NotNull(message = "数据库名不能为空")
    private String db;

    /**
     * 表描述
     */
    @NotBlank(message = "表名不能为空")
    private String comment;

    /**
     * 表字段
     */
    @NotNull(message = "表名不能为空")
    @Valid
    private List<FieldMetaCmd> fields;
}
