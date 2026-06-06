package com.cyan.dataman.application.metadata.quality;

import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityAlertBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleSuggestionBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleTemplateBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRunBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualitySummaryBO;
import com.cyan.dataman.application.metadata.quality.cmd.MetadataQualityRuleCmd;

import java.util.List;

/**
 * 元数据质量应用服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface MetadataQualityService {

    /**
     * 查询规则模板
     */
    List<MetadataQualityRuleTemplateBO> listRuleTemplates();

    /**
     * 查询质量汇总
     */
    MetadataQualitySummaryBO getSummary(String tableId);

    /**
     * 查询表规则
     */
    List<MetadataQualityRuleBO> listRules(String tableId);

    /**
     * 创建规则
     */
    MetadataQualityRuleBO createRule(String tableId, MetadataQualityRuleCmd cmd);

    /**
     * 更新规则
     */
    MetadataQualityRuleBO updateRule(String tableId, String ruleId, MetadataQualityRuleCmd cmd);

    /**
     * 删除规则
     */
    void deleteRule(String tableId, String ruleId);

    /**
     * 推荐规则
     */
    List<MetadataQualityRuleBO> recommendRules(String tableId);

    /**
     * 流式推荐规则候选
     */
    List<MetadataQualityRuleSuggestionBO> recommendRulesStream(String tableId, QualityRuleRecommendStreamListener listener);

    /**
     * 立即运行质量检查
     */
    MetadataQualityRunBO run(String tableId);

    /**
     * 查询运行列表
     */
    List<MetadataQualityRunBO> listRuns(String tableId);

    /**
     * 查询运行详情
     */
    MetadataQualityRunBO getRunDetail(String runId);

    /**
     * 查询表告警
     */
    List<MetadataQualityAlertBO> listAlerts(String tableId);

    /**
     * 关闭告警
     */
    MetadataQualityAlertBO closeAlert(String alertId, String operator);
}
