package com.cyan.dataman.adapter.ds.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 索引信息 DTO
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class IndexDTO {

    /**
     * 索引名称
     */
    @NotBlank(message = "索引名称不能为空")
    private String name;

    /**
     * 索引类型（PRIMARY, UNIQUE, INDEX, FULLTEXT）
     */
    @NotBlank(message = "索引类型不能为空")
    private String indexType;

    /**
     * 索引字段列表
     */
    @NotEmpty(message = "索引字段不能为空")
    private List<String> fieldNames;

    /**
     * 索引方法（BTREE, HASH, GIN, GIST）
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
}
