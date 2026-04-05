package com.cyan.dataman.application.ds.cmd;

import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.IndexValObj;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 表结构命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableSchemaCmd {

    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 字段列表
     */
    @NotEmpty(message = "字段列表不能为空")
    @Valid
    private List<ColumnValObj> columns;

    /**
     * 索引列表
     */
    @Valid
    private List<IndexValObj> indexes;
}
