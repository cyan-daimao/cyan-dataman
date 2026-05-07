package com.cyan.dataman.application.metadata.cmd;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 创建表关系命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class CreateRelationCmd {

    /**
     * 源表 catalog
     */
    @NotBlank(message = "源表 catalog 不能为空")
    private String sourceCatalog;

    /**
     * 源表 schema
     */
    @NotBlank(message = "源表 schema 不能为空")
    private String sourceSchema;

    /**
     * 源表名
     */
    @NotBlank(message = "源表名不能为空")
    private String sourceTable;

    /**
     * 源表字段
     */
    @NotBlank(message = "源表字段不能为空")
    private String sourceColumn;

    /**
     * 目标表 catalog
     */
    @NotBlank(message = "目标表 catalog 不能为空")
    private String targetCatalog;

    /**
     * 目标表 schema
     */
    @NotBlank(message = "目标表 schema 不能为空")
    private String targetSchema;

    /**
     * 目标表名
     */
    @NotBlank(message = "目标表名不能为空")
    private String targetTable;

    /**
     * 目标表字段
     */
    @NotBlank(message = "目标表字段不能为空")
    private String targetColumn;

    /**
     * JOIN类型：LEFT/INNER/RIGHT
     */
    @NotBlank(message = "JOIN 类型不能为空")
    private String joinType;

    /**
     * 描述
     */
    private String description;
}
