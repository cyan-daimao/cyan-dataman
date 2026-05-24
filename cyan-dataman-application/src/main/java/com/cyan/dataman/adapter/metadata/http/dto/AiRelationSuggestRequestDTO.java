package com.cyan.dataman.adapter.metadata.http.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * AI 关联推荐请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class AiRelationSuggestRequestDTO {

    /**
     * 当前表 catalog
     */
    @NotBlank(message = "catalog不能为空")
    private String catalog;

    /**
     * 当前表 schema
     */
    @NotBlank(message = "schema不能为空")
    private String schema;

    /**
     * 当前表名
     */
    @NotBlank(message = "table不能为空")
    private String table;

    /**
     * 最大推荐数量
     */
    private Integer maxCandidates;
}
