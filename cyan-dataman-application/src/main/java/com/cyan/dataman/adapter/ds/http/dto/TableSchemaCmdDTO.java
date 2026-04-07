package com.cyan.dataman.adapter.ds.http.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 表结构命令 DTO（用于创建/更新表请求）
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class TableSchemaCmdDTO {

    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;

    /**
     * 表注释
     */
    @NotBlank(message = "表注释不能为空")
    private String tableComment;

    /**
     * 字段列表
     */
    @NotEmpty(message = "字段列表不能为空")
    @Valid
    private List<ColumnDTO> columns;

    /**
     * 索引列表
     */
    @Valid
    private List<IndexDTO> indexes;
}
