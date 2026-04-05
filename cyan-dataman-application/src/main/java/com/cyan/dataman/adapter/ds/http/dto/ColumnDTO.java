package com.cyan.dataman.adapter.ds.http.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 字段信息 DTO（用于 API 请求/响应）
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class ColumnDTO {

    /**
     * 字段名称
     */
    @NotBlank(message = "字段名称不能为空")
    private String name;

    /**
     * 字段类型（原始数据库类型字符串）
     */
    @NotBlank(message = "字段类型不能为空")
    private String type;

    /**
     * 字段注释
     */
    @NotBlank(message = "字段注释不能为空")
    private String comment;

    /**
     * 字段是否为空
     */
    private Boolean nullable;

    /**
     * 字段是否自增
     */
    private Boolean autoIncrement;

    /**
     * 字段默认值
     */
    private String defaultValue;

    /**
     * 精度
     */
    private Integer precision;

    /**
     * 小数位数
     */
    private Integer scale;

    // ==================== MySQL 特有字段 ====================

    /**
     * 无符号标识（MySQL）
     */
    private Boolean unsigned;

    /**
     * 零填充标识（MySQL）
     */
    private Boolean zerofill;

    /**
     * 字符集（MySQL）
     */
    private String charset;

    /**
     * 排序规则（MySQL）
     */
    private String collation;

    // ==================== PostgreSQL 特有字段 ====================

    /**
     * 数组维度（PostgreSQL）
     */
    private Integer arrayDimensions;

    /**
     * 时区标识（PostgreSQL）
     */
    private Boolean withTimeZone;
}
