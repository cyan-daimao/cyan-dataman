package com.cyan.dataman.domain.metadata.valobj;

import com.cyan.dataman.enums.ColumnDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 字段信息
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ColumnValObj {
    /**
     * 字段名称
     */
    @NotBlank(message = "字段名称不能为空")
    private String name;

    /**
     * 字段类型
     */
    @NotNull(message = "字段类型不能为空")
    private ColumnDataType type;

    /**
     * 字段注释
     */
    @NotBlank(message = "字段注释不能为空")
    private String comment;

    /**
     * 字段是否为空
     */
    @NotNull(message = "字段是否为空不能为空")
    private Boolean nullable;

    /**
     * 字段是否自增
     */
    @NotNull(message = "字段是否自增不能为空")
    private Boolean autoIncrement;

    /**
     * 字段默认值
     */
    private String defaultValue;

    /**
     * Decimal精度（仅DECIMAL类型使用）
     */
    private Integer precision;

    /**
     * Decimal小数位数（仅DECIMAL类型使用）
     */
    private Integer scale;
}