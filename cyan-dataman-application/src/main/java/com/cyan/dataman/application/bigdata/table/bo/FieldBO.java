package com.cyan.dataman.application.bigdata.table.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 字段业务信息
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FieldBO {

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 字段描述
     */
    private String comment;

    /**
     * 分区
     */
    private String pt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
