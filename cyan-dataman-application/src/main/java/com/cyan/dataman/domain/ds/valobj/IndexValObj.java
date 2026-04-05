package com.cyan.dataman.domain.ds.valobj;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 数据源索引信息
 *
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
     * 索引类型（如 PRIMARY, UNIQUE, INDEX, FULLTEXT 等）
     */
    @NotBlank(message = "索引类型不能为空")
    private String indexType;

    /**
     * 索引字段列表
     */
    @NotEmpty(message = "索引字段不能为空")
    private List<String> fieldNames;

    /**
     * 索引方法（如 BTREE, HASH 等）
     */
    private String indexMethod;

    /**
     * 索引注释
     */
    private String comment;

    /**
     * 是否唯一索引
     */
    private Boolean unique;

    /**
     * 是否主键
     */
    private Boolean primaryKey;

    /**
     * 判断是否为主键索引
     */
    public boolean isPrimaryKey() {
        return primaryKey != null && primaryKey;
    }

    /**
     * 判断是否为唯一索引
     */
    public boolean isUnique() {
        return unique != null && unique;
    }
}
