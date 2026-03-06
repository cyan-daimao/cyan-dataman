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
 * 索引信息
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class IndexValObj {
    /**
     * 索引名称
     */
    @NotBlank(message = "索引名称不能为空")
    private String name;

    /**
     * 索引类型
     */
    @NotBlank(message = "索引类型不能为空")
    private String indexType;

    /**
     * 索引字段
     */
    @NotEmpty(message = "索引字段不能为空")
    private List<String> fieldNames;
}
