package com.cyan.dataman.application.metadata;

import com.cyan.dataman.application.metadata.bo.AiRelationSuggestionBO;

import java.util.List;

/**
 * AI 关联推荐流式监听器
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface AiRelationSuggestStreamListener {

    /**
     * 输出状态信息
     *
     * @param message 状态信息
     */
    void onStatus(String message);

    /**
     * 输出 AI 原始回复片段
     *
     * @param content 回复片段
     */
    void onAnswer(String content);

    /**
     * 输出推荐结果
     *
     * @param suggestions 推荐结果
     */
    void onResult(List<AiRelationSuggestionBO> suggestions);
}
