package com.cyan.dataman.adapter.http.dto;

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
public class IndexDTO {
    /**
     * 索引名称
     */
    private String name;

    /**
     * 索引类型
     */
    private String indexType;

    /**
     * 索引字段
     */
    private List<String> fieldNames;
}
