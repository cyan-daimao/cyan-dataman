package com.cyan.dataman.application.metadata;

import com.cyan.dataman.application.metadata.bo.AiRelationSuggestionBO;

import java.util.List;

/**
 * AI 关联推荐服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface AiRelationSuggestService {

    /**
     * 推荐表关联关系
     *
     * @param catalog       当前表 catalog
     * @param schema        当前表 schema
     * @param table         当前表名
     * @param maxCandidates 最大返回候选数
     * @return 推荐候选列表
     */
    List<AiRelationSuggestionBO> suggest(String catalog, String schema, String table, Integer maxCandidates);
}
