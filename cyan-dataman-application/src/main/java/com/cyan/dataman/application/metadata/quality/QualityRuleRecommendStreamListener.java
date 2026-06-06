package com.cyan.dataman.application.metadata.quality;

import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleSuggestionBO;

import java.util.List;

/**
 * 数据质量规则推荐流式监听器
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface QualityRuleRecommendStreamListener {

    /**
     * 输出状态信息
     */
    void onStatus(String message);

    /**
     * 输出AI原始回复片段
     */
    void onAnswer(String content);

    /**
     * 输出推荐候选
     */
    void onSuggestions(List<MetadataQualityRuleSuggestionBO> suggestions);
}
