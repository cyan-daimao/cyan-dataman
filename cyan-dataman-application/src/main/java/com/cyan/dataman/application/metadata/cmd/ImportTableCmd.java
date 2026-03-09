package com.cyan.dataman.application.metadata.cmd;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 导入表命令
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ImportTableCmd {
    /**
     * 目录
     */
    @NotBlank(message = "目录不能为空")
    private String catalog;

    /**
     * 库
     */
    @NotBlank(message = "库不能为空")
    private String schema;

    /**
     * 表
     */
    @NotBlank(message = "表不能为空")
    private String table;
}
