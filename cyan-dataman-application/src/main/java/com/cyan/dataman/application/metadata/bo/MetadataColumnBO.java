package com.cyan.dataman.application.metadata.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 元数据表字段业务对象
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataColumnBO {

    /**
     * 主键
     */
    private String id;

    /**
     * 字段名
     */
    private String col;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 字段注释
     */
    private String comment;

    /**
     * 可空
     */
    private Boolean nullable;

    /**
     * 秘密等级
     */
    private String secretLevel;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 是否自增
     */
    private Boolean autoIncrement;
}
