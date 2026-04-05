package com.cyan.dataman.domain.ds.valobj;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 数据源字段信息基类
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "databaseType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MysqlColumnValObj.class, name = "MYSQL"),
        @JsonSubTypes.Type(value = PgsqlColumnValObj.class, name = "POSTGRESQL")
})
public abstract class ColumnValObj {

    /**
     * 字段名称
     */
    @NotBlank(message = "字段名称不能为空")
    protected String name;

    /**
     * 字段类型（原始数据库类型字符串）
     */
    @NotBlank(message = "字段类型不能为空")
    protected String type;

    /**
     * 字段注释
     */
    @NotBlank(message = "字段注释不能为空")
    protected String comment;

    /**
     * 字段是否为空
     */
    protected Boolean nullable;

    /**
     * 字段是否自增
     */
    protected Boolean autoIncrement;

    /**
     * 字段默认值
     */
    protected String defaultValue;

    /**
     * 精度（如 DECIMAL(10,2) 中的 10，或 VARCHAR(255) 中的 255）
     */
    protected Integer precision;

    /**
     * 小数位数（仅 DECIMAL 类型使用）
     */
    protected Integer scale;

    /**
     * 数据库类型标识（用于序列化/反序列化）
     */
    protected String databaseType;

    /**
     * 获取类型枚举代码
     *
     * @return 类型枚举代码
     */
    public abstract String getTypeEnumCode();
}
