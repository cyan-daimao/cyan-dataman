package com.cyan.dataman.domain.metadata.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 元数据列表查询
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class MetadataTableListQuery {

    /**
     * 主键
     */
    private List<String> ids;

    /**
     * 表名（模糊搜索）
     */
    private String name;

    /**
     * 表描述（模糊搜索）
     */
    private String comment;

    /**
     * 搜索内容（同时匹配表名或描述）
     */
    private String content;
}
