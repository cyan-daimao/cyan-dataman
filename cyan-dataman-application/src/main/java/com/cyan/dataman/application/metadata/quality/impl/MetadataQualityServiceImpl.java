package com.cyan.dataman.application.metadata.quality.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.metadata.quality.MetadataQualityService;
import com.cyan.dataman.application.metadata.quality.QualityRuleRecommendStreamListener;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityAlertBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityResultBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleSuggestionBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleTemplateBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRunBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualitySummaryBO;
import com.cyan.dataman.application.metadata.quality.cmd.MetadataQualityRuleCmd;
import com.cyan.dataman.application.metadata.quality.convert.MetadataQualityAppConvert;
import com.cyan.dataman.domain.metadata.MetadataTable;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityAlert;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityResult;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityRule;
import com.cyan.dataman.domain.metadata.quality.MetadataQualityRun;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityAlertRepository;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityResultRepository;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityRuleRepository;
import com.cyan.dataman.domain.metadata.quality.repository.MetadataQualityRunRepository;
import com.cyan.dataman.domain.metadata.repository.MetadataTableRepository;
import com.cyan.dataman.domain.metadata.valobj.ColumnValObj;
import com.cyan.dataman.domain.metadata.valobj.IndexValObj;
import com.cyan.dataman.infra.gateway.DifyQualityGateway;
import com.cyan.dataman.infra.util.StarRocksUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 元数据质量应用服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Slf4j
@Service
public class MetadataQualityServiceImpl implements MetadataQualityService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final MetadataTableRepository metadataTableRepository;
    private final MetadataQualityRuleRepository ruleRepository;
    private final MetadataQualityRunRepository runRepository;
    private final MetadataQualityResultRepository resultRepository;
    private final MetadataQualityAlertRepository alertRepository;
    private final StarRocksUtil starRocksUtil;
    private final DifyQualityGateway difyQualityGateway;

    public MetadataQualityServiceImpl(MetadataTableRepository metadataTableRepository,
                                      MetadataQualityRuleRepository ruleRepository,
                                      MetadataQualityRunRepository runRepository,
                                      MetadataQualityResultRepository resultRepository,
                                      MetadataQualityAlertRepository alertRepository,
                                      StarRocksUtil starRocksUtil,
                                      DifyQualityGateway difyQualityGateway) {
        this.metadataTableRepository = metadataTableRepository;
        this.ruleRepository = ruleRepository;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.alertRepository = alertRepository;
        this.starRocksUtil = starRocksUtil;
        this.difyQualityGateway = difyQualityGateway;
    }

    /**
     * 查询规则模板
     */
    @Override
    public List<MetadataQualityRuleTemplateBO> listRuleTemplates() {
        return List.of(
                template("TABLE_ROW_COUNT", "表级", "表行数检查", "检查表行数是否满足固定阈值", false, "{\"operator\":\">=\",\"expected\":1}"),
                template("CONDITION_MATCH_RATE", "表级", "条件匹配率", "检查满足指定条件的数据占比", false, "{\"condition\":\"status = 'SUCCESS'\",\"operator\":\">=\",\"expected\":0.99}"),
                template("NULL_COUNT", "空值", "空值行数", "检查字段空值行数是否满足阈值", true, "{\"operator\":\"<=\",\"expected\":0}"),
                template("NULL_COUNT_ZERO", "空值", "空值行数为0", "检查字段是否不存在空值", true, "{}"),
                template("NULL_RATE", "空值", "空值率", "检查字段空值占比是否满足阈值", true, "{\"operator\":\"<=\",\"expected\":0.01}"),
                template("REGEX_FORMAT", "格式校验", "正则格式校验", "检查字段值是否匹配指定正则", true, "{\"pattern\":\"^1[3-9][0-9]{9}$\",\"allowNull\":false}"),
                template("DATE_FORMAT", "格式校验", "日期格式校验", "检查字段值是否符合日期格式", true, "{\"pattern\":\"^[0-9]{4}-[0-9]{2}-[0-9]{2}$\",\"allowNull\":true}"),
                template("EMAIL_FORMAT", "格式校验", "邮箱格式校验", "检查字段值是否符合邮箱格式", true, "{\"allowNull\":true}"),
                template("ID_CARD_FORMAT", "格式校验", "身份证格式校验", "检查字段值是否符合身份证号码格式", true, "{\"allowNull\":true}"),
                template("MOBILE_FORMAT", "格式校验", "手机号格式校验", "检查字段值是否符合中国大陆手机号格式", true, "{\"allowNull\":true}"),
                template("CURRENCY_FORMAT", "格式校验", "金额格式校验", "检查字段值是否符合金额格式", true, "{\"allowNull\":true}"),
                template("NUMERIC_FORMAT", "格式校验", "数值格式校验", "检查字段值是否符合数值格式", true, "{\"allowNull\":true}"),
                template("PHONE_FORMAT", "格式校验", "电话格式校验", "检查字段值是否符合电话格式", true, "{\"allowNull\":true}"),
                template("DUPLICATE_COUNT", "重复/唯一", "重复值行数", "检查字段重复值行数是否满足阈值", true, "{\"operator\":\"<=\",\"expected\":0}"),
                template("DUPLICATE_COUNT_ZERO", "重复/唯一", "重复值行数为0", "检查字段是否不存在重复值", true, "{}"),
                template("DUPLICATE_RATE", "重复/唯一", "重复值率", "检查字段重复值占比是否满足阈值", true, "{\"operator\":\"<=\",\"expected\":0.01}"),
                template("MULTI_FIELD_DUPLICATE_COUNT_ZERO", "重复/唯一", "多字段联合重复值为0", "检查多个字段组合是否不存在重复值", false, "{\"columns\":[\"user_id\",\"order_id\"]}"),
                template("DISTINCT_COUNT", "重复/唯一", "唯一值数", "检查字段唯一值数量是否满足阈值", true, "{\"operator\":\">=\",\"expected\":1}"),
                template("DISTINCT_RATE", "重复/唯一", "唯一值率", "检查字段唯一值占比是否满足阈值", true, "{\"operator\":\">=\",\"expected\":0.8}"),
                template("MIN_VALUE", "统计值", "最小值", "检查字段最小值是否满足阈值", true, "{\"operator\":\">=\",\"expected\":0}"),
                template("MAX_VALUE", "统计值", "最大值", "检查字段最大值是否满足阈值", true, "{\"operator\":\"<=\",\"expected\":100}"),
                template("AVG_VALUE", "统计值", "平均值", "检查字段平均值是否满足阈值", true, "{\"min\":0,\"max\":100}"),
                template("SUM_VALUE", "统计值", "汇总值", "检查字段汇总值是否满足阈值", true, "{\"operator\":\">=\",\"expected\":0}"),
                template("FRESHNESS", "及时性", "数据新鲜度", "检查日期时间字段最大值距离当前时间是否超出阈值", true, "{\"maxDelayMinutes\":1440}"),
                template("ENUM_MISMATCH_COUNT", "枚举/离散", "枚举不匹配行数", "检查字段值不在枚举集合内的行数", true, "{\"values\":[\"A\",\"B\"],\"allowNull\":true,\"operator\":\"<=\",\"expected\":0}"),
                template("ENUM_MISMATCH_COUNT_ZERO", "枚举/离散", "枚举不匹配行数为0", "检查字段值是否都在枚举集合内", true, "{\"values\":[\"A\",\"B\"],\"allowNull\":true}"),
                template("ENUM_MISMATCH_DISTINCT_COUNT", "枚举/离散", "枚举不匹配去重数", "检查不在枚举集合内的不同取值数量", true, "{\"values\":[\"A\",\"B\"],\"allowNull\":true,\"operator\":\"<=\",\"expected\":0}"),
                template("DISCRETE_GROUP_COUNT", "枚举/离散", "离散值分组数", "检查字段离散取值分组数量", true, "{\"operator\":\"<=\",\"expected\":100}"),
                template("CUSTOM_SQL", "自定义", "自定义SQL检查", "执行返回 fail_count 的自定义SQL", false, "{\"sql\":\"select 0 as fail_count, count(1) as total_count from iceberg.ods.example_table\"}")
        );
    }

    /**
     * 查询质量汇总
     */
    @Override
    public MetadataQualitySummaryBO getSummary(String tableId) {
        List<MetadataQualityRule> rules = ruleRepository.listByTableId(tableId);
        MetadataQualityRun latestRun = runRepository.findLatestByTableId(tableId);
        List<MetadataQualityAlert> alerts = alertRepository.listByTableId(tableId);
        MetadataQualitySummaryBO summary = new MetadataQualitySummaryBO()
                .setTableId(tableId)
                .setScore(BigDecimal.ZERO)
                .setRuleCount(rules.size())
                .setEnabledRuleCount((int) rules.stream().filter(rule -> Boolean.TRUE.equals(rule.getEnabled())).count())
                .setOpenAlertCount((int) alerts.stream().filter(alert -> "OPEN".equals(alert.getStatus())).count())
                .setPassCount(0)
                .setWarnCount(0)
                .setFailCount(0);
        if (latestRun != null) {
            summary.setLatestRunId(latestRun.getId() == null ? null : latestRun.getId().toString())
                    .setLatestRunStatus(latestRun.getStatus())
                    .setLatestRunTime(latestRun.getStartedAt())
                    .setScore(latestRun.getScore())
                    .setPassCount(latestRun.getPassCount())
                    .setWarnCount(latestRun.getWarnCount())
                    .setFailCount(latestRun.getFailCount());
        }
        return summary;
    }

    /**
     * 查询表规则
     */
    @Override
    public List<MetadataQualityRuleBO> listRules(String tableId) {
        return MetadataQualityAppConvert.INSTANCE.toRuleBOList(ruleRepository.listByTableId(tableId));
    }

    /**
     * 创建规则
     */
    @Override
    public MetadataQualityRuleBO createRule(String tableId, MetadataQualityRuleCmd cmd) {
        MetadataTable table = requireTable(tableId);
        validateRuleCmd(table, cmd);
        MetadataQualityRule rule = MetadataQualityAppConvert.INSTANCE.toRule(cmd).setTableId(tableId);
        return MetadataQualityAppConvert.INSTANCE.toRuleBO(rule.save(ruleRepository));
    }

    /**
     * 更新规则
     */
    @Override
    public MetadataQualityRuleBO updateRule(String tableId, String ruleId, MetadataQualityRuleCmd cmd) {
        MetadataTable table = requireTable(tableId);
        MetadataQualityRule oldRule = requireRule(tableId, ruleId);
        validateRuleCmd(table, cmd);
        MetadataQualityRule rule = MetadataQualityAppConvert.INSTANCE.toRule(cmd)
                .setId(oldRule.getId())
                .setTableId(tableId)
                .setCreatedAt(oldRule.getCreatedAt());
        return MetadataQualityAppConvert.INSTANCE.toRuleBO(rule.update(ruleRepository));
    }

    /**
     * 删除规则
     */
    @Override
    public void deleteRule(String tableId, String ruleId) {
        MetadataQualityRule rule = requireRule(tableId, ruleId);
        rule.delete(ruleRepository);
    }

    /**
     * 推荐规则
     */
    @Override
    public List<MetadataQualityRuleBO> recommendRules(String tableId) {
        MetadataTable table = requireTable(tableId);
        List<MetadataQualityRule> existingRules = ruleRepository.listByTableId(tableId);
        List<MetadataQualityRuleSuggestionBO> suggestions = buildFallbackSuggestions(table, existingRules);
        List<MetadataQualityRule> createdRules = new ArrayList<>();
        for (MetadataQualityRuleSuggestionBO suggestion : suggestions) {
            if (!Boolean.TRUE.equals(suggestion.getExists())) {
                createdRules.add(saveSuggestion(tableId, suggestion));
            }
        }
        if (createdRules.isEmpty()) {
            return MetadataQualityAppConvert.INSTANCE.toRuleBOList(existingRules);
        }
        List<MetadataQualityRule> allRules = new ArrayList<>(existingRules);
        allRules.addAll(createdRules);
        allRules.sort(Comparator.comparing(MetadataQualityRule::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return MetadataQualityAppConvert.INSTANCE.toRuleBOList(allRules);
    }

    /**
     * 流式推荐规则候选
     */
    @Override
    public List<MetadataQualityRuleSuggestionBO> recommendRulesStream(String tableId, QualityRuleRecommendStreamListener listener) {
        MetadataTable table = requireTable(tableId);
        List<MetadataQualityRule> existingRules = ruleRepository.listByTableId(tableId);
        sendStatus(listener, "正在读取表结构和已有质量规则");
        List<MetadataQualityRuleSuggestionBO> fallbackSuggestions = buildFallbackSuggestions(table, existingRules);
        if (!difyQualityGateway.available()) {
            sendStatus(listener, "Dify 数据质量推荐未启用或配置不完整，使用基础规则推荐");
            sendSuggestions(listener, fallbackSuggestions);
            return fallbackSuggestions;
        }
        try {
            sendStatus(listener, "正在调用 AI 分析数据质量规则");
            String answer = difyQualityGateway.streamSuggestRules(buildQualityPrompt(table, existingRules), listener::onAnswer);
            sendStatus(listener, "正在解析 AI 推荐结果");
            List<MetadataQualityRuleSuggestionBO> aiSuggestions = parseAiSuggestions(table, existingRules, answer);
            if (aiSuggestions.isEmpty()) {
                sendStatus(listener, "AI 未返回有效候选，使用基础规则推荐");
                sendSuggestions(listener, fallbackSuggestions);
                return fallbackSuggestions;
            }
            sendSuggestions(listener, aiSuggestions);
            return aiSuggestions;
        } catch (Exception e) {
            log.warn("AI 数据质量规则推荐失败, tableId: {}", tableId, e);
            sendStatus(listener, "AI 推荐不可用，使用基础规则推荐：" + Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName()));
            sendSuggestions(listener, fallbackSuggestions);
            return fallbackSuggestions;
        }
    }

    /**
     * 立即运行质量检查
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetadataQualityRunBO run(String tableId) {
        MetadataTable table = requireTable(tableId);
        List<MetadataQualityRule> rules = ruleRepository.listEnabledByTableId(tableId);
        MetadataQualityRun run = new MetadataQualityRun().setTableId(tableId).save(runRepository);
        List<MetadataQualityResult> results = new ArrayList<>();
        try {
            for (MetadataQualityRule rule : rules) {
                results.add(executeRule(table, rule, run.getId()));
            }
            results = resultRepository.saveBatch(results);
            createAlerts(tableId, run.getId(), results, rules);
            fillRunCount(run, results, rules.size());
            run = run.finish(runRepository);
            return toRunDetailBO(run, results);
        } catch (Exception e) {
            log.error("数据质量运行失败, tableId: {}, runId: {}", tableId, run.getId(), e);
            run = run.fail(runRepository, e.getMessage());
            return toRunDetailBO(run, results);
        }
    }

    /**
     * 查询运行列表
     */
    @Override
    public List<MetadataQualityRunBO> listRuns(String tableId) {
        return MetadataQualityAppConvert.INSTANCE.toRunBOList(runRepository.listByTableId(tableId, 30));
    }

    /**
     * 查询运行详情
     */
    @Override
    public MetadataQualityRunBO getRunDetail(String runId) {
        MetadataQualityRun run = runRepository.findById(Long.valueOf(runId));
        Assert.notNull(run, new SilentException("质量运行不存在"));
        List<MetadataQualityResult> results = resultRepository.listByRunId(run.getId());
        return toRunDetailBO(run, results);
    }

    /**
     * 查询表告警
     */
    @Override
    public List<MetadataQualityAlertBO> listAlerts(String tableId) {
        return MetadataQualityAppConvert.INSTANCE.toAlertBOList(alertRepository.listByTableId(tableId));
    }

    /**
     * 关闭告警
     */
    @Override
    public MetadataQualityAlertBO closeAlert(String alertId, String operator) {
        MetadataQualityAlert alert = alertRepository.findById(Long.valueOf(alertId));
        Assert.notNull(alert, new SilentException("质量告警不存在"));
        return MetadataQualityAppConvert.INSTANCE.toAlertBO(alert.close(alertRepository, operator));
    }

    /**
     * 执行单条规则
     */
    private MetadataQualityResult executeRule(MetadataTable table, MetadataQualityRule rule, Long runId) {
        MetadataQualityResult result = baseResult(rule, runId);
        try {
            validateRuleField(table, rule);
            String ruleType = normalizeRuleType(rule.getRuleType());
            return switch (ruleType) {
                case "TABLE_ROW_COUNT" -> executeTableRowCount(table, rule, result);
                case "CONDITION_MATCH_RATE" -> executeConditionMatchRate(table, rule, result);
                case "NULL_COUNT", "NULL_COUNT_ZERO", "NULL_RATE" -> executeNullMetric(table, rule, result, ruleType);
                case "REGEX_FORMAT", "DATE_FORMAT", "EMAIL_FORMAT", "ID_CARD_FORMAT", "MOBILE_FORMAT",
                        "CURRENCY_FORMAT", "NUMERIC_FORMAT", "PHONE_FORMAT" -> executeRegexFormat(table, rule, result, ruleType);
                case "DUPLICATE_COUNT", "DUPLICATE_COUNT_ZERO", "DUPLICATE_RATE" -> executeDuplicateMetric(table, rule, result, ruleType);
                case "MULTI_FIELD_DUPLICATE_COUNT_ZERO" -> executeMultiFieldDuplicateZero(table, rule, result);
                case "DISTINCT_COUNT", "DISTINCT_RATE" -> executeDistinctMetric(table, rule, result, ruleType);
                case "MIN_VALUE", "MAX_VALUE", "AVG_VALUE", "SUM_VALUE" -> executeAggregateMetric(table, rule, result, ruleType);
                case "ENUM_MISMATCH_COUNT", "ENUM_MISMATCH_COUNT_ZERO", "ENUM_MISMATCH_DISTINCT_COUNT" -> executeEnumMismatchMetric(table, rule, result, ruleType);
                case "DISCRETE_GROUP_COUNT" -> executeDiscreteGroupCount(table, rule, result);
                case "FIELD_VALUE_RANGE" -> executeFieldValueRange(table, rule, result);
                case "FRESHNESS" -> executeFreshness(table, rule, result);
                case "CUSTOM_SQL" -> executeCustomSql(rule, result);
                default -> result.setStatus("FAIL")
                        .setDetailJson(JSON.toJSONString(Map.of("error", "不支持的规则类型: " + rule.getRuleType())));
            };
        } catch (Exception e) {
            return result.setStatus("FAIL")
                    .setActualValue("执行异常")
                    .setDetailJson(JSON.toJSONString(Map.of("error", Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName()))));
        }
    }

    /**
     * 执行行数规则
     */
    private MetadataQualityResult executeTableRowCount(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        JSONObject config = config(rule);
        Long minCount = config.getLong("minCount");
        Long maxCount = config.getLong("maxCount");
        long total = firstLong(query("select count(1) as total_count from " + tableRef(table) + where(rule)), "total_count");
        boolean passed = minCount != null || maxCount != null
                ? (minCount == null || total >= minCount) && (maxCount == null || total <= maxCount)
                : compareMetric(BigDecimal.valueOf(total), config, BigDecimal.ONE, ">=");
        String expected = minCount != null || maxCount != null ? expectedRange(minCount, maxCount) : expectedCompare(config, ">= 1");
        return finish(result, rule, passed, total, passed ? 0L : 1L,
                String.valueOf(total), expected, null);
    }

    /**
     * 执行条件匹配率规则
     */
    private MetadataQualityResult executeConditionMatchRate(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        JSONObject config = config(rule);
        String condition = config.getString("condition");
        Assert.notBlank(condition, new SilentException("CONDITION_MATCH_RATE 规则 condition 不能为空"));
        String sql = "select count(1) as total_count, "
                + "sum(case when " + condition + " then 1 else 0 end) as match_count, "
                + "sum(case when " + condition + " then 0 else 1 end) as fail_count, "
                + "case when count(1) = 0 then 0 else cast(sum(case when " + condition + " then 1 else 0 end) as double) / count(1) end as actual_rate from "
                + tableRef(table) + where(rule);
        Map<String, Object> row = firstRow(query(sql));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        BigDecimal actual = toBigDecimal(row.get("actual_rate"));
        boolean passed = compareMetric(actual, config, BigDecimal.ONE, ">=");
        String invalid = "(not (" + condition + ") or (" + condition + ") is null)";
        return finish(result, rule, passed, total, fail, decimalText(actual), expectedCompare(config, ">= 1"),
                "select * from " + tableRef(table) + appendCondition(rule, invalid) + " limit 100");
    }

    /**
     * 执行空值指标规则
     */
    private MetadataQualityResult executeNullMetric(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result,
                                                    String ruleType) throws SQLException {
        String column = columnRef(rule.getColumnName());
        String sql = "select count(1) as total_count, "
                + "sum(case when " + column + " is null then 1 else 0 end) as fail_count, "
                + "case when count(1) = 0 then 0 else cast(sum(case when " + column + " is null then 1 else 0 end) as double) / count(1) end as actual_rate from "
                + tableRef(table) + where(rule);
        Map<String, Object> row = firstRow(query(sql));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        BigDecimal actual = "NULL_RATE".equals(ruleType) ? toBigDecimal(row.get("actual_rate")) : BigDecimal.valueOf(fail);
        boolean passed = "NULL_COUNT_ZERO".equals(ruleType)
                ? fail == 0
                : compareMetric(actual, config(rule), BigDecimal.ZERO, "<=");
        return finish(result, rule, passed, total, fail, decimalText(actual), "NULL_COUNT_ZERO".equals(ruleType) ? "0" : expectedCompare(config(rule), "<= 0"),
                "select * from " + tableRef(table) + appendCondition(rule, column + " is null") + " limit 100");
    }

    /**
     * 执行格式校验规则
     */
    private MetadataQualityResult executeRegexFormat(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result,
                                                     String ruleType) throws SQLException {
        JSONObject config = config(rule);
        String pattern = patternForRule(ruleType, config);
        Boolean allowNull = config.getBoolean("allowNull");
        String column = columnRef(rule.getColumnName());
        String regexp = "regexp_like(cast(" + column + " as varchar), '" + escapeSql(pattern) + "')";
        String invalid = Boolean.FALSE.equals(allowNull)
                ? "(" + column + " is null or not " + regexp + ")"
                : "(" + column + " is not null and not " + regexp + ")";
        Map<String, Object> row = firstRow(query("select count(1) as total_count, sum(case when " + invalid
                + " then 1 else 0 end) as fail_count from " + tableRef(table) + where(rule)));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        return finish(result, rule, fail == 0, total, fail, String.valueOf(fail), "0",
                "select * from " + tableRef(table) + appendCondition(rule, invalid) + " limit 100");
    }

    /**
     * 执行重复值指标规则
     */
    private MetadataQualityResult executeDuplicateMetric(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result,
                                                         String ruleType) throws SQLException {
        String column = columnRef(rule.getColumnName());
        String sql = "select count(1) as total_count, count(1) - count(distinct " + column + ") as fail_count, "
                + "case when count(1) = 0 then 0 else cast(count(1) - count(distinct " + column + ") as double) / count(1) end as actual_rate from "
                + tableRef(table) + where(rule);
        Map<String, Object> row = firstRow(query(sql));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        BigDecimal actual = "DUPLICATE_RATE".equals(ruleType) ? toBigDecimal(row.get("actual_rate")) : BigDecimal.valueOf(fail);
        boolean passed = "DUPLICATE_COUNT_ZERO".equals(ruleType)
                ? fail == 0
                : compareMetric(actual, config(rule), BigDecimal.ZERO, "<=");
        return finish(result, rule, passed, total, fail, decimalText(actual), "DUPLICATE_COUNT_ZERO".equals(ruleType) ? "0" : expectedCompare(config(rule), "<= 0"),
                "select " + column + ", count(1) as duplicate_count from " + tableRef(table)
                        + where(rule) + " group by " + column + " having count(1) > 1 limit 100");
    }

    /**
     * 执行多字段联合重复值为0规则
     */
    private MetadataQualityResult executeMultiFieldDuplicateZero(MetadataTable table, MetadataQualityRule rule,
                                                                 MetadataQualityResult result) throws SQLException {
        List<String> columns = configColumns(rule);
        String columnRefs = columns.stream().map(this::columnRef).reduce((a, b) -> a + ", " + b).orElse("");
        String sql = "select (select count(1) from " + tableRef(table) + where(rule) + ") as total_count, "
                + "(select count(1) from (select " + columnRefs + " from " + tableRef(table) + where(rule)
                + " group by " + columnRefs + " having count(1) > 1) t) as fail_count";
        Map<String, Object> row = firstRow(query(sql));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        return finish(result, rule, fail == 0, total, fail, String.valueOf(fail), "0",
                "select " + columnRefs + ", count(1) as duplicate_count from " + tableRef(table)
                        + where(rule) + " group by " + columnRefs + " having count(1) > 1 limit 100");
    }

    /**
     * 执行唯一值指标规则
     */
    private MetadataQualityResult executeDistinctMetric(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result,
                                                        String ruleType) throws SQLException {
        String column = columnRef(rule.getColumnName());
        String sql = "select count(1) as total_count, count(distinct " + column + ") as distinct_count, "
                + "case when count(1) = 0 then 0 else cast(count(distinct " + column + ") as double) / count(1) end as distinct_rate from "
                + tableRef(table) + where(rule);
        Map<String, Object> row = firstRow(query(sql));
        long total = toLong(row.get("total_count"));
        BigDecimal actual = "DISTINCT_RATE".equals(ruleType) ? toBigDecimal(row.get("distinct_rate")) : BigDecimal.valueOf(toLong(row.get("distinct_count")));
        boolean passed = compareMetric(actual, config(rule), null, null);
        return finish(result, rule, passed, total, passed ? 0L : 1L, decimalText(actual), expectedCompare(config(rule), "按配置阈值"), null);
    }

    /**
     * 执行统计值规则
     */
    private MetadataQualityResult executeAggregateMetric(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result,
                                                         String ruleType) throws SQLException {
        String function = switch (ruleType) {
            case "MIN_VALUE" -> "min";
            case "MAX_VALUE" -> "max";
            case "AVG_VALUE" -> "avg";
            case "SUM_VALUE" -> "sum";
            default -> throw new SilentException("不支持的统计规则: " + ruleType);
        };
        String column = columnRef(rule.getColumnName());
        Map<String, Object> row = firstRow(query("select count(1) as total_count, " + function + "(" + column + ") as actual_value from "
                + tableRef(table) + where(rule)));
        long total = toLong(row.get("total_count"));
        BigDecimal actual = toBigDecimal(row.get("actual_value"));
        boolean passed = compareMetric(actual, config(rule), null, null);
        return finish(result, rule, passed, total, passed ? 0L : 1L, decimalText(actual), expectedCompare(config(rule), "按配置阈值"), null);
    }

    /**
     * 执行枚举不匹配指标规则
     */
    private MetadataQualityResult executeEnumMismatchMetric(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result,
                                                            String ruleType) throws SQLException {
        JSONObject config = config(rule);
        String invalid = enumInvalidCondition(rule, config);
        String column = columnRef(rule.getColumnName());
        String actualSql = "ENUM_MISMATCH_DISTINCT_COUNT".equals(ruleType)
                ? "count(distinct case when " + invalid + " then cast(" + column + " as varchar) end)"
                : "sum(case when " + invalid + " then 1 else 0 end)";
        Map<String, Object> row = firstRow(query("select count(1) as total_count, " + actualSql + " as fail_count from "
                + tableRef(table) + where(rule)));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        boolean passed = "ENUM_MISMATCH_COUNT_ZERO".equals(ruleType)
                ? fail == 0
                : compareMetric(BigDecimal.valueOf(fail), config, BigDecimal.ZERO, "<=");
        return finish(result, rule, passed, total, fail, String.valueOf(fail),
                "ENUM_MISMATCH_COUNT_ZERO".equals(ruleType) ? "0" : expectedCompare(config, "<= 0"),
                "select * from " + tableRef(table) + appendCondition(rule, invalid) + " limit 100");
    }

    /**
     * 执行离散分组数规则
     */
    private MetadataQualityResult executeDiscreteGroupCount(MetadataTable table, MetadataQualityRule rule,
                                                            MetadataQualityResult result) throws SQLException {
        String column = columnRef(rule.getColumnName());
        Map<String, Object> row = firstRow(query("select count(1) as total_count, count(distinct " + column + ") as group_count from "
                + tableRef(table) + where(rule)));
        long total = toLong(row.get("total_count"));
        BigDecimal actual = BigDecimal.valueOf(toLong(row.get("group_count")));
        boolean passed = compareMetric(actual, config(rule), null, null);
        return finish(result, rule, passed, total, passed ? 0L : 1L, decimalText(actual), expectedCompare(config(rule), "按配置阈值"), null);
    }

    /**
     * 执行字段值范围规则
     */
    private MetadataQualityResult executeFieldValueRange(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        JSONObject config = config(rule);
        String column = columnRef(rule.getColumnName());
        List<String> conditions = new ArrayList<>();
        if (config.containsKey("min")) {
            conditions.add(column + " < " + sqlLiteral(config.get("min")));
        }
        if (config.containsKey("max")) {
            conditions.add(column + " > " + sqlLiteral(config.get("max")));
        }
        Assert.isTrue(!conditions.isEmpty(), new SilentException("FIELD_VALUE_RANGE 规则 min/max 不能同时为空"));
        String invalid = Boolean.FALSE.equals(config.getBoolean("allowNull"))
                ? "(" + column + " is null or " + String.join(" or ", conditions) + ")"
                : column + " is not null and (" + String.join(" or ", conditions) + ")";
        Map<String, Object> row = firstRow(query("select count(1) as total_count, sum(case when " + invalid
                + " then 1 else 0 end) as fail_count from " + tableRef(table) + where(rule)));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        return finish(result, rule, fail == 0, total, fail, String.valueOf(fail), config.toJSONString(),
                "select * from " + tableRef(table) + appendCondition(rule, invalid) + " limit 100");
    }

    /**
     * 执行及时性规则
     */
    private MetadataQualityResult executeFreshness(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        JSONObject config = config(rule);
        Long maxDelayMinutes = config.getLong("maxDelayMinutes");
        if (maxDelayMinutes == null && config.getLong("maxDelayHours") != null) {
            maxDelayMinutes = config.getLong("maxDelayHours") * 60;
        }
        Assert.notNull(maxDelayMinutes, new SilentException("FRESHNESS 规则 maxDelayMinutes 不能为空"));
        String column = columnRef(rule.getColumnName());
        String sql = "select timestampdiff(MINUTE, max(" + column + "), now()) as delay_minutes from "
                + tableRef(table) + where(rule);
        long delayMinutes = firstLong(query(sql), "delay_minutes");
        boolean passed = delayMinutes <= maxDelayMinutes;
        return finish(result, rule, passed, 1L, passed ? 0L : 1L,
                delayMinutes + "分钟", "<= " + maxDelayMinutes + "分钟", null);
    }

    /**
     * 执行自定义SQL规则
     */
    private MetadataQualityResult executeCustomSql(MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        String sql = config(rule).getString("sql");
        Assert.notBlank(sql, new SilentException("CUSTOM_SQL 规则 sql 不能为空"));
        Map<String, Object> row = firstRow(query(sql));
        long fail = toLong(row.get("fail_count"));
        long total = row.containsKey("total_count") ? toLong(row.get("total_count")) : 0L;
        return finish(result, rule, fail == 0, total, fail, String.valueOf(fail), "0", sql);
    }

    /**
     * 完成规则结果
     */
    private MetadataQualityResult finish(MetadataQualityResult result, MetadataQualityRule rule, boolean passed, Long total,
                                         Long fail, String actual, String expected, String sampleSql) {
        String status = passed ? "PASS" : ("FAIL".equals(rule.getSeverity()) ? "FAIL" : "WARN");
        return result.setStatus(status)
                .setTotalCount(total)
                .setFailCount(fail)
                .setActualValue(actual)
                .setExpectedValue(expected)
                .setSampleSql(sampleSql)
                .setDetailJson(JSON.toJSONString(Map.of("severity", rule.getSeverity())));
    }

    /**
     * 创建失败告警
     */
    private void createAlerts(String tableId, Long runId, List<MetadataQualityResult> results, List<MetadataQualityRule> rules) {
        Map<Long, MetadataQualityRule> ruleMap = rules.stream().filter(rule -> rule.getId() != null)
                .collect(java.util.stream.Collectors.toMap(MetadataQualityRule::getId, rule -> rule, (a, b) -> a));
        for (MetadataQualityResult result : results) {
            MetadataQualityRule rule = ruleMap.get(result.getRuleId());
            if (rule != null && "FAIL".equals(result.getStatus()) && "FAIL".equals(rule.getSeverity())) {
                new MetadataQualityAlert()
                        .setTableId(tableId)
                        .setRunId(runId)
                        .setResultId(result.getId())
                        .setRuleId(rule.getId())
                        .setTitle(rule.getRuleName() + " 失败")
                        .setMessage("实际值: " + result.getActualValue() + "，期望值: " + result.getExpectedValue())
                        .setSeverity(rule.getSeverity())
                        .save(alertRepository);
            }
        }
    }

    /**
     * 填充运行统计
     */
    private void fillRunCount(MetadataQualityRun run, List<MetadataQualityResult> results, int enabledRuleCount) {
        int passCount = (int) results.stream().filter(result -> "PASS".equals(result.getStatus())).count();
        int warnCount = (int) results.stream().filter(result -> "WARN".equals(result.getStatus())).count();
        int failCount = (int) results.stream().filter(result -> "FAIL".equals(result.getStatus())).count();
        BigDecimal score = enabledRuleCount == 0
                ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(passCount * 100.0 / enabledRuleCount).setScale(2, RoundingMode.HALF_UP);
        run.setPassCount(passCount)
                .setWarnCount(warnCount)
                .setFailCount(failCount)
                .setScore(score);
    }

    /**
     * 转换运行详情
     */
    private MetadataQualityRunBO toRunDetailBO(MetadataQualityRun run, List<MetadataQualityResult> results) {
        List<MetadataQualityResultBO> resultBOList = MetadataQualityAppConvert.INSTANCE.toResultBOList(results);
        return MetadataQualityAppConvert.INSTANCE.toRunBO(run).setResults(resultBOList);
    }

    /**
     * 创建模板
     */
    private MetadataQualityRuleTemplateBO template(String type, String dimension, String name, String description,
                                                  boolean columnRequired, String configExample) {
        return new MetadataQualityRuleTemplateBO()
                .setRuleType(type)
                .setDimension(dimension)
                .setName(name)
                .setDescription(description)
                .setColumnRequired(columnRequired)
                .setConfigExample(configExample);
    }

    /**
     * 构建基础推荐候选
     */
    private List<MetadataQualityRuleSuggestionBO> buildFallbackSuggestions(MetadataTable table, List<MetadataQualityRule> existingRules) {
        Map<String, MetadataQualityRuleSuggestionBO> suggestions = new LinkedHashMap<>();
        Set<String> existingKeys = existingSuggestionKeys(existingRules);
        addSuggestion(suggestions, existingKeys, "TABLE_ROW_COUNT", "表级", "表行数检查",
                null, "{\"operator\":\">=\",\"expected\":1}", null, "WARN",
                "基础表级规则，用于发现空表或异常无数据。", BigDecimal.valueOf(0.70));
        for (ColumnValObj column : columns(table)) {
            String type = normalizeType(column.getType());
            String searchText = normalizeType(column.getName() + " " + Optional.ofNullable(column.getComment()).orElse(""));
            addSuggestion(suggestions, existingKeys, "NULL_RATE", "空值", column.getName() + " 空值率检查",
                    column.getName(), "{\"operator\":\"<=\",\"expected\":0.1}", null, "WARN",
                    "基础字段规则，用于发现字段空值占比异常。", BigDecimal.valueOf(0.62));
            if (Boolean.FALSE.equals(column.getNullable())) {
                addSuggestion(suggestions, existingKeys, "NULL_COUNT_ZERO", "空值", column.getName() + " 空值行数为0",
                        column.getName(), "{}", null, "FAIL",
                        "字段元数据标记为不可为空，适合作为强规则监控空值。", BigDecimal.valueOf(0.88));
            }
            if (searchText.contains("mobile") || searchText.contains("phone") || searchText.contains("手机号")) {
                addSuggestion(suggestions, existingKeys, "MOBILE_FORMAT", "格式校验", column.getName() + " 手机号格式校验",
                        column.getName(), "{\"allowNull\":true}", null, "WARN",
                        "字段名或注释包含手机号/phone/mobile，适合做手机号格式校验。", BigDecimal.valueOf(0.86));
            }
            if (searchText.contains("id_card") || searchText.contains("idcard") || searchText.contains("身份证")) {
                addSuggestion(suggestions, existingKeys, "ID_CARD_FORMAT", "格式校验", column.getName() + " 身份证格式校验",
                        column.getName(), "{\"allowNull\":true}", null, "WARN",
                        "字段名或注释包含身份证/id_card，适合做身份证格式校验。", BigDecimal.valueOf(0.86));
            }
            if (searchText.contains("email") || searchText.contains("邮箱")) {
                addSuggestion(suggestions, existingKeys, "EMAIL_FORMAT", "格式校验", column.getName() + " 邮箱格式校验",
                        column.getName(), "{\"allowNull\":true}", null, "WARN",
                        "字段名或注释包含邮箱/email，适合做邮箱格式校验。", BigDecimal.valueOf(0.86));
            }
            if (isDateTimeType(type)) {
                addSuggestion(suggestions, existingKeys, "FRESHNESS", "及时性", column.getName() + " 及时性检查",
                        column.getName(), "{\"maxDelayMinutes\":1440}", null, "WARN",
                        "字段类型为日期时间，适合监控数据新鲜度。", BigDecimal.valueOf(0.78));
            }
            if (isNumericType(type)) {
                addSuggestion(suggestions, existingKeys, "MIN_VALUE", "统计值", column.getName() + " 最小值检查",
                        column.getName(), "{\"operator\":\">=\",\"expected\":0}", null, "WARN",
                        "字段类型为数值，适合做基础统计阈值监控。", BigDecimal.valueOf(0.66));
            }
        }
        for (IndexValObj index : Optional.ofNullable(table.getTable().getIndexes()).orElse(List.of())) {
            String indexType = Optional.ofNullable(index.getIndexType()).orElse("").toUpperCase(Locale.ROOT);
            if ((indexType.contains("UNIQUE") || indexType.contains("PRIMARY"))
                    && index.getFieldNames() != null && index.getFieldNames().size() == 1) {
                String columnName = index.getFieldNames().get(0);
                if (hasColumn(table, columnName)) {
                    addSuggestion(suggestions, existingKeys, "DUPLICATE_COUNT_ZERO", "重复/唯一", columnName + " 重复值行数为0",
                            columnName, "{}", null, "FAIL",
                            "字段来自主键或唯一索引，适合作为唯一性强规则。", BigDecimal.valueOf(0.90));
                }
            }
        }
        return new ArrayList<>(suggestions.values());
    }

    /**
     * 添加推荐候选
     */
    private void addSuggestion(Map<String, MetadataQualityRuleSuggestionBO> suggestions, Set<String> existingKeys,
                               String type, String dimension, String name, String column, String configJson,
                               String filterSql, String severity, String reason, BigDecimal confidence) {
        String key = ruleSuggestionKey(type, column, configJson);
        if (suggestions.containsKey(key)) {
            return;
        }
        suggestions.put(key, new MetadataQualityRuleSuggestionBO()
                .setRuleType(normalizeRuleType(type))
                .setDimension(dimension)
                .setRuleName(name)
                .setColumnName(column)
                .setConfigJson(configJson)
                .setFilterSql(filterSql)
                .setSeverity(severity)
                .setReason(reason)
                .setConfidence(confidence)
                .setExists(existingKeys.contains(key)));
    }

    /**
     * 保存推荐候选
     */
    private MetadataQualityRule saveSuggestion(String tableId, MetadataQualityRuleSuggestionBO suggestion) {
        return new MetadataQualityRule()
                .setTableId(tableId)
                .setRuleType(suggestion.getRuleType())
                .setDimension(suggestion.getDimension())
                .setColumnName(suggestion.getColumnName())
                .setRuleName(suggestion.getRuleName())
                .setConfigJson(Optional.ofNullable(suggestion.getConfigJson()).filter(this::hasText).orElse("{}"))
                .setFilterSql(suggestion.getFilterSql())
                .setSeverity(Optional.ofNullable(suggestion.getSeverity()).filter(this::hasText).orElse("WARN"))
                .setEnabled(true)
                .save(ruleRepository);
    }

    /**
     * 构建AI提示词
     */
    private String buildQualityPrompt(MetadataTable table, List<MetadataQualityRule> existingRules) {
        JSONObject payload = new JSONObject();
        payload.put("currentTable", toPromptTable(table));
        payload.put("existingRules", existingRules.stream().map(this::toPromptRule).toList());
        payload.put("ruleTemplates", listRuleTemplates().stream().map(this::toPromptTemplate).toList());
        return """
                你是数据治理平台的数据质量规则推荐助手。请根据输入的元数据表结构、已有质量规则和可用规则模板，推荐适合当前表的数据质量规则。

                输出要求：
                1. 必须只输出严格 JSON，不要输出 Markdown、解释文字或代码块。
                2. JSON 顶层格式为 {"suggestions":[...]}。
                3. 每个 suggestions 元素必须包含 ruleType、dimension、ruleName、configJson、severity、reason、confidence，可选 columnName、filterSql、exists。
                4. ruleType 只能使用 ruleTemplates 中提供的类型。
                5. 字段级规则的 columnName 必须来自 currentTable.columns。
                6. configJson 必须是单行 JSON 字符串，字符串内部的双引号必须转义；多字段唯一规则通过 configJson.columns 返回。
                7. severity 只能是 WARN 或 FAIL。强约束用 FAIL，探索性统计规则用 WARN。
                8. 不要推荐 existingRules 中已有的重复规则；如果判断重复但仍返回，请设置 exists=true。
                9. CUSTOM_SQL 必须返回 fail_count，可选 total_count，并在 SQL 内自行写 WHERE 范围。
                10. 不要输出 <think>、</think> 或任何思考过程文本。

                推荐重点：
                - 不可空字段推荐 NULL_COUNT_ZERO。
                - 主键或唯一索引字段推荐 DUPLICATE_COUNT_ZERO。
                - 手机号、身份证、邮箱等字段推荐对应格式规则。
                - 日期时间字段推荐 FRESHNESS。
                - 普通字段可推荐 NULL_RATE，数值字段可推荐统计值规则。

                输入数据：
                %s
                """.formatted(JSON.toJSONString(payload));
    }

    /**
     * 表结构转为提示词对象
     */
    private JSONObject toPromptTable(MetadataTable table) {
        JSONObject object = new JSONObject();
        object.put("catalog", table.getTable().getCatalog());
        object.put("schema", table.getTable().getSchema());
        object.put("table", table.getName());
        object.put("fullName", table.getTable().getCatalog() + "." + table.getTable().getSchema() + "." + table.getName());
        object.put("comment", table.getComment());
        object.put("columns", columns(table).stream().map(column -> new JSONObject()
                .fluentPut("name", column.getName())
                .fluentPut("type", column.getType())
                .fluentPut("comment", column.getComment())
                .fluentPut("nullable", column.getNullable())
        ).toList());
        object.put("indexes", Optional.ofNullable(table.getTable().getIndexes()).orElse(List.of()).stream().map(index -> new JSONObject()
                .fluentPut("name", index.getName())
                .fluentPut("indexType", index.getIndexType())
                .fluentPut("fieldNames", index.getFieldNames())
        ).toList());
        return object;
    }

    /**
     * 已有规则转为提示词对象
     */
    private JSONObject toPromptRule(MetadataQualityRule rule) {
        return new JSONObject()
                .fluentPut("ruleType", normalizeRuleType(rule.getRuleType()))
                .fluentPut("dimension", rule.getDimension())
                .fluentPut("ruleName", rule.getRuleName())
                .fluentPut("columnName", rule.getColumnName())
                .fluentPut("configJson", rule.getConfigJson())
                .fluentPut("filterSql", rule.getFilterSql())
                .fluentPut("severity", rule.getSeverity());
    }

    /**
     * 规则模板转为提示词对象
     */
    private JSONObject toPromptTemplate(MetadataQualityRuleTemplateBO template) {
        return new JSONObject()
                .fluentPut("ruleType", template.getRuleType())
                .fluentPut("dimension", template.getDimension())
                .fluentPut("name", template.getName())
                .fluentPut("description", template.getDescription())
                .fluentPut("columnRequired", template.getColumnRequired())
                .fluentPut("configExample", template.getConfigExample());
    }

    /**
     * 解析AI推荐候选
     */
    private List<MetadataQualityRuleSuggestionBO> parseAiSuggestions(MetadataTable table, List<MetadataQualityRule> existingRules, String answer) {
        if (!hasText(answer)) {
            return List.of();
        }
        String json = extractJson(answer);
        JSONArray array;
        if (json.startsWith("[")) {
            array = JSON.parseArray(json);
        } else {
            JSONObject root = JSON.parseObject(json);
            array = root.getJSONArray("suggestions");
        }
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        Set<String> existingKeys = existingSuggestionKeys(existingRules);
        Map<String, MetadataQualityRuleSuggestionBO> validSuggestions = new LinkedHashMap<>();
        for (Object item : array) {
            if (!(item instanceof JSONObject object)) {
                continue;
            }
            Optional<MetadataQualityRuleSuggestionBO> suggestion = parseAiSuggestion(table, existingKeys, object);
            suggestion.ifPresent(value -> validSuggestions.put(ruleSuggestionKey(value.getRuleType(), value.getColumnName(), value.getConfigJson()), value));
        }
        return new ArrayList<>(validSuggestions.values());
    }

    /**
     * 解析单条AI推荐候选
     */
    private Optional<MetadataQualityRuleSuggestionBO> parseAiSuggestion(MetadataTable table, Set<String> existingKeys, JSONObject object) {
        String ruleType = normalizeRuleType(object.getString("ruleType"));
        if (!supportedRuleTypes().contains(ruleType)) {
            return Optional.empty();
        }
        String columnName = object.getString("columnName");
        if (singleColumnRuleTypes().contains(ruleType) && !hasColumn(table, columnName)) {
            return Optional.empty();
        }
        String configJson = normalizeConfigJson(object.get("configJson"));
        JSONObject config = JSON.parseObject(configJson);
        if ("MULTI_FIELD_DUPLICATE_COUNT_ZERO".equals(ruleType)) {
            List<String> columns = configColumns(config);
            if (columns.isEmpty() || columns.stream().anyMatch(column -> !hasColumn(table, column))) {
                return Optional.empty();
            }
        }
        String severity = Optional.ofNullable(object.getString("severity")).orElse("WARN").toUpperCase(Locale.ROOT);
        if (!List.of("WARN", "FAIL").contains(severity)) {
            severity = "WARN";
        }
        String key = ruleSuggestionKey(ruleType, columnName, configJson);
        return Optional.of(new MetadataQualityRuleSuggestionBO()
                .setRuleType(ruleType)
                .setDimension(Optional.ofNullable(object.getString("dimension")).filter(this::hasText).orElse(defaultDimension(ruleType)))
                .setRuleName(Optional.ofNullable(object.getString("ruleName")).filter(this::hasText).orElse(defaultRuleName(ruleType, columnName)))
                .setColumnName(columnName)
                .setConfigJson(configJson)
                .setFilterSql(object.getString("filterSql"))
                .setSeverity(severity)
                .setReason(object.getString("reason"))
                .setConfidence(Optional.ofNullable(object.getBigDecimal("confidence")).orElse(BigDecimal.valueOf(0.80)))
                .setExists(Boolean.TRUE.equals(object.getBoolean("exists")) || existingKeys.contains(key)));
    }

    /**
     * 标准化配置JSON
     */
    private String normalizeConfigJson(Object configValue) {
        if (configValue == null) {
            return "{}";
        }
        if (configValue instanceof JSONObject || configValue instanceof JSONArray) {
            return JSON.toJSONString(configValue);
        }
        String text = String.valueOf(configValue);
        if (!hasText(text)) {
            return "{}";
        }
        JSON.parseObject(text);
        return text;
    }

    /**
     * 提取JSON内容
     */
    private String extractJson(String answer) {
        String text = answer.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "");
            int fenceIndex = text.lastIndexOf("```");
            if (fenceIndex >= 0) {
                text = text.substring(0, fenceIndex).trim();
            }
        }
        int thinkEnd = text.lastIndexOf("</think>");
        if (thinkEnd >= 0) {
            text = text.substring(thinkEnd + "</think>".length()).trim();
        }
        for (int index = 0; index < text.length(); index++) {
            char start = text.charAt(index);
            if (start != '{' && start != '[') {
                continue;
            }
            int end = findJsonEnd(text, index);
            if (end < 0) {
                continue;
            }
            String candidate = normalizeAiJsonText(text.substring(index, end + 1));
            try {
                if (start == '[') {
                    JSON.parseArray(candidate);
                } else {
                    JSON.parseObject(candidate);
                }
                return candidate;
            } catch (Exception ignored) {
                // 继续尝试后续 JSON 片段，AI 可能在回答前面输出了调试或思考文本。
            }
        }
        throw new SilentException("AI 返回内容不包含可解析 JSON");
    }

    /**
     * 定位JSON片段结束位置
     */
    private int findJsonEnd(String text, int startIndex) {
        List<Character> stack = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        for (int index = startIndex; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
                continue;
            }
            if (ch == '{') {
                stack.add('}');
            } else if (ch == '[') {
                stack.add(']');
            } else if (ch == '}' || ch == ']') {
                if (stack.isEmpty() || !Objects.equals(stack.get(stack.size() - 1), ch)) {
                    return -1;
                }
                stack.remove(stack.size() - 1);
                if (stack.isEmpty()) {
                    return index;
                }
            }
        }
        return -1;
    }

    /**
     * 归一化AI返回的JSON文本
     */
    private String normalizeAiJsonText(String json) {
        StringBuilder builder = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < json.length(); index++) {
            char ch = json.charAt(index);
            if (inString) {
                if (escaped) {
                    builder.append(ch);
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    builder.append(ch);
                    escaped = true;
                    continue;
                }
                if (ch == '"') {
                    builder.append(ch);
                    inString = false;
                    continue;
                }
                if (ch == '\n') {
                    builder.append("\\n");
                    continue;
                }
                if (ch == '\r') {
                    builder.append("\\r");
                    continue;
                }
                if (ch == '\t') {
                    builder.append("\\t");
                    continue;
                }
                builder.append(ch);
                continue;
            }
            builder.append(ch);
            if (ch == '"') {
                inString = true;
            }
        }
        return builder.toString().trim();
    }

    /**
     * 已有规则推荐键
     */
    private Set<String> existingSuggestionKeys(List<MetadataQualityRule> existingRules) {
        Set<String> existingKeys = new HashSet<>();
        Optional.ofNullable(existingRules).orElse(List.of())
                .forEach(rule -> existingKeys.add(ruleSuggestionKey(rule.getRuleType(), rule.getColumnName(), rule.getConfigJson())));
        return existingKeys;
    }

    /**
     * 推荐候选去重键
     */
    private String ruleSuggestionKey(String type, String columnName, String configJson) {
        String ruleType = normalizeRuleType(type);
        if ("MULTI_FIELD_DUPLICATE_COUNT_ZERO".equals(ruleType)) {
            List<String> columns = configColumns(safeParseConfig(configJson));
            List<String> sortedColumns = new ArrayList<>(columns);
            sortedColumns.sort(String::compareTo);
            return ruleType + ":" + String.join(",", sortedColumns);
        }
        return ruleType + ":" + Optional.ofNullable(columnName).orElse("");
    }

    /**
     * 安全解析配置JSON
     */
    private JSONObject safeParseConfig(String configJson) {
        if (!hasText(configJson)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(configJson);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /**
     * 默认维度
     */
    private String defaultDimension(String ruleType) {
        return listRuleTemplates().stream()
                .filter(template -> Objects.equals(template.getRuleType(), ruleType))
                .map(MetadataQualityRuleTemplateBO::getDimension)
                .findFirst()
                .orElse("自定义");
    }

    /**
     * 默认规则名称
     */
    private String defaultRuleName(String ruleType, String columnName) {
        String prefix = hasText(columnName) ? columnName + " " : "";
        return prefix + ruleType;
    }

    /**
     * 发送状态事件
     */
    private void sendStatus(QualityRuleRecommendStreamListener listener, String message) {
        if (listener != null) {
            listener.onStatus(message);
        }
    }

    /**
     * 发送推荐候选事件
     */
    private void sendSuggestions(QualityRuleRecommendStreamListener listener, List<MetadataQualityRuleSuggestionBO> suggestions) {
        if (listener != null) {
            listener.onSuggestions(suggestions);
        }
    }

    /**
     * 创建推荐规则
     */
    private void addRecommendedRule(String tableId, List<MetadataQualityRule> createdRules, Set<String> existingKeys,
                                    String type, String dimension, String column, String name, String severity, String configJson) {
        String key = ruleKey(type, column);
        if (existingKeys.contains(key)) {
            return;
        }
        existingKeys.add(key);
        MetadataQualityRule rule = new MetadataQualityRule()
                .setTableId(tableId)
                .setRuleType(type)
                .setDimension(dimension)
                .setColumnName(column)
                .setRuleName(name)
                .setSeverity(severity)
                .setEnabled(true)
                .setConfigJson(configJson)
                .save(ruleRepository);
        createdRules.add(rule);
    }

    /**
     * 校验规则命令
     */
    private void validateRuleCmd(MetadataTable table, MetadataQualityRuleCmd cmd) {
        Assert.notNull(cmd, new SilentException("规则配置不能为空"));
        Assert.notBlank(cmd.getRuleType(), new SilentException("规则类型不能为空"));
        Assert.notBlank(cmd.getRuleName(), new SilentException("规则名称不能为空"));
        Assert.notBlank(cmd.getDimension(), new SilentException("质量维度不能为空"));
        Assert.isTrue(List.of("WARN", "FAIL").contains(cmd.getSeverity()), new SilentException("严重等级只能是 WARN/FAIL"));
        String ruleType = normalizeRuleType(cmd.getRuleType());
        Assert.isTrue(supportedRuleTypes().contains(ruleType), new SilentException("不支持的规则类型: " + cmd.getRuleType()));
        JSONObject config = new JSONObject();
        if (hasText(cmd.getConfigJson())) {
            try {
                config = JSON.parseObject(cmd.getConfigJson());
            } catch (Exception e) {
                throw new SilentException("规则配置JSON格式不正确");
            }
        }
        if (singleColumnRuleTypes().contains(ruleType)) {
            Assert.notBlank(cmd.getColumnName(), new SilentException(cmd.getRuleType() + " 规则字段不能为空"));
            Assert.isTrue(hasColumn(table, cmd.getColumnName()), new SilentException("字段不存在: " + cmd.getColumnName()));
        }
        if ("MULTI_FIELD_DUPLICATE_COUNT_ZERO".equals(ruleType)) {
            List<String> columns = configColumns(config);
            Assert.isTrue(!columns.isEmpty(), new SilentException("MULTI_FIELD_DUPLICATE_COUNT_ZERO 规则 columns 不能为空"));
            for (String column : columns) {
                Assert.isTrue(hasColumn(table, column), new SilentException("字段不存在: " + column));
            }
        }
    }

    /**
     * 查询元数据表
     */
    private MetadataTable requireTable(String tableId) {
        MetadataTable table = metadataTableRepository.findById(tableId);
        Assert.notNull(table, new SilentException("元数据表不存在"));
        Assert.notNull(table.getTable(), new SilentException("元数据表结构不存在"));
        return table;
    }

    /**
     * 查询规则
     */
    private MetadataQualityRule requireRule(String tableId, String ruleId) {
        MetadataQualityRule rule = ruleRepository.findById(Long.valueOf(ruleId));
        Assert.notNull(rule, new SilentException("质量规则不存在"));
        Assert.isTrue(Objects.equals(tableId, rule.getTableId()), new SilentException("质量规则不属于当前表"));
        return rule;
    }

    /**
     * 生成基础结果
     */
    private MetadataQualityResult baseResult(MetadataQualityRule rule, Long runId) {
        return new MetadataQualityResult()
                .setRunId(runId)
                .setRuleId(rule.getId())
                .setRuleName(rule.getRuleName())
                .setRuleType(rule.getRuleType())
                .setDimension(rule.getDimension())
                .setColumnName(rule.getColumnName())
                .setStatus("PASS")
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 校验规则字段
     */
    private void validateRuleField(MetadataTable table, MetadataQualityRule rule) {
        String ruleType = normalizeRuleType(rule.getRuleType());
        if (singleColumnRuleTypes().contains(ruleType)) {
            Assert.isTrue(hasColumn(table, rule.getColumnName()), new SilentException("字段不存在: " + rule.getColumnName()));
        }
        if ("MULTI_FIELD_DUPLICATE_COUNT_ZERO".equals(ruleType)) {
            for (String column : configColumns(rule)) {
                Assert.isTrue(hasColumn(table, column), new SilentException("字段不存在: " + column));
            }
        }
    }

    /**
     * 查询SQL
     */
    private List<Map<String, Object>> query(String sql) throws SQLException {
        return starRocksUtil.queryForList(sql);
    }

    /**
     * 获取首行
     */
    private Map<String, Object> firstRow(List<Map<String, Object>> rows) {
        return Optional.ofNullable(rows).orElse(List.of()).stream().findFirst().orElse(Map.of());
    }

    /**
     * 获取首行Long值
     */
    private long firstLong(List<Map<String, Object>> rows, String key) {
        return toLong(firstRow(rows).get(key));
    }

    /**
     * 转换Long值
     */
    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 解析规则配置
     */
    private JSONObject config(MetadataQualityRule rule) {
        if (!hasText(rule.getConfigJson())) {
            return new JSONObject();
        }
        return JSON.parseObject(rule.getConfigJson());
    }

    /**
     * 归一化规则类型
     */
    private String normalizeRuleType(String ruleType) {
        return switch (Optional.ofNullable(ruleType).orElse("").toUpperCase(Locale.ROOT)) {
            case "ROW_COUNT" -> "TABLE_ROW_COUNT";
            case "NOT_NULL" -> "NULL_COUNT_ZERO";
            case "UNIQUE" -> "DUPLICATE_COUNT_ZERO";
            case "ENUM" -> "ENUM_MISMATCH_COUNT_ZERO";
            case "RANGE" -> "FIELD_VALUE_RANGE";
            default -> Optional.ofNullable(ruleType).orElse("").toUpperCase(Locale.ROOT);
        };
    }

    /**
     * 支持的规则类型
     */
    private Set<String> supportedRuleTypes() {
        return Set.of(
                "TABLE_ROW_COUNT", "CONDITION_MATCH_RATE",
                "NULL_COUNT", "NULL_COUNT_ZERO", "NULL_RATE",
                "REGEX_FORMAT", "DATE_FORMAT", "EMAIL_FORMAT", "ID_CARD_FORMAT", "MOBILE_FORMAT",
                "CURRENCY_FORMAT", "NUMERIC_FORMAT", "PHONE_FORMAT",
                "DUPLICATE_COUNT", "DUPLICATE_COUNT_ZERO", "DUPLICATE_RATE", "MULTI_FIELD_DUPLICATE_COUNT_ZERO",
                "DISTINCT_COUNT", "DISTINCT_RATE",
                "MIN_VALUE", "MAX_VALUE", "AVG_VALUE", "SUM_VALUE",
                "ENUM_MISMATCH_COUNT", "ENUM_MISMATCH_COUNT_ZERO", "ENUM_MISMATCH_DISTINCT_COUNT", "DISCRETE_GROUP_COUNT",
                "FIELD_VALUE_RANGE", "FRESHNESS", "CUSTOM_SQL"
        );
    }

    /**
     * 单字段规则类型
     */
    private Set<String> singleColumnRuleTypes() {
        return Set.of(
                "NULL_COUNT", "NULL_COUNT_ZERO", "NULL_RATE",
                "REGEX_FORMAT", "DATE_FORMAT", "EMAIL_FORMAT", "ID_CARD_FORMAT", "MOBILE_FORMAT",
                "CURRENCY_FORMAT", "NUMERIC_FORMAT", "PHONE_FORMAT",
                "DUPLICATE_COUNT", "DUPLICATE_COUNT_ZERO", "DUPLICATE_RATE",
                "DISTINCT_COUNT", "DISTINCT_RATE",
                "MIN_VALUE", "MAX_VALUE", "AVG_VALUE", "SUM_VALUE",
                "ENUM_MISMATCH_COUNT", "ENUM_MISMATCH_COUNT_ZERO", "ENUM_MISMATCH_DISTINCT_COUNT",
                "DISCRETE_GROUP_COUNT", "FIELD_VALUE_RANGE", "FRESHNESS"
        );
    }

    /**
     * 从规则中读取多字段配置
     */
    private List<String> configColumns(MetadataQualityRule rule) {
        return configColumns(config(rule));
    }

    /**
     * 从JSON中读取多字段配置
     */
    private List<String> configColumns(JSONObject config) {
        JSONArray columns = config.getJSONArray("columns");
        if (columns == null) {
            return List.of();
        }
        return columns.stream().map(String::valueOf).filter(this::hasText).toList();
    }

    /**
     * 枚举不匹配条件
     */
    private String enumInvalidCondition(MetadataQualityRule rule, JSONObject config) {
        JSONArray values = config.getJSONArray("values");
        Assert.isTrue(values != null && !values.isEmpty(), new SilentException("枚举规则 values 不能为空"));
        String inValues = values.stream().map(value -> "'" + escapeSql(String.valueOf(value)) + "'").reduce((a, b) -> a + "," + b).orElse("''");
        String column = columnRef(rule.getColumnName());
        String mismatch = "cast(" + column + " as varchar) not in (" + inValues + ")";
        return Boolean.FALSE.equals(config.getBoolean("allowNull"))
                ? "(" + column + " is null or " + mismatch + ")"
                : "(" + column + " is not null and " + mismatch + ")";
    }

    /**
     * 获取格式校验正则
     */
    private String patternForRule(String ruleType, JSONObject config) {
        String configuredPattern = config.getString("pattern");
        if (hasText(configuredPattern)) {
            return configuredPattern;
        }
        return switch (ruleType) {
            case "DATE_FORMAT" -> "^[0-9]{4}-[0-9]{2}-[0-9]{2}$";
            case "EMAIL_FORMAT" -> "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            case "ID_CARD_FORMAT" -> "^[1-9][0-9]{5}(18|19|20)[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[0-9]{3}[0-9Xx]$";
            case "MOBILE_FORMAT" -> "^1[3-9][0-9]{9}$";
            case "CURRENCY_FORMAT" -> "^-?[0-9]+(\\.[0-9]{1,2})?$";
            case "NUMERIC_FORMAT" -> "^-?[0-9]+(\\.[0-9]+)?$";
            case "PHONE_FORMAT" -> "^(0[0-9]{2,3}-?)?[0-9]{7,8}$|^1[3-9][0-9]{9}$";
            default -> throw new SilentException("REGEX_FORMAT 规则 pattern 不能为空");
        };
    }

    /**
     * 判断指标是否满足阈值
     */
    private boolean compareMetric(BigDecimal actual, JSONObject config, BigDecimal defaultExpected, String defaultOperator) {
        if (config.containsKey("min") || config.containsKey("max")) {
            BigDecimal min = config.getBigDecimal("min");
            BigDecimal max = config.getBigDecimal("max");
            return (min == null || actual.compareTo(min) >= 0) && (max == null || actual.compareTo(max) <= 0);
        }
        String operator = Optional.ofNullable(config.getString("operator")).orElse(defaultOperator);
        BigDecimal expected = config.getBigDecimal("expected");
        if (expected == null) {
            expected = defaultExpected;
        }
        if (!hasText(operator) || expected == null) {
            return true;
        }
        return switch (operator) {
            case ">" -> actual.compareTo(expected) > 0;
            case ">=" -> actual.compareTo(expected) >= 0;
            case "<" -> actual.compareTo(expected) < 0;
            case "<=" -> actual.compareTo(expected) <= 0;
            case "=", "==" -> actual.compareTo(expected) == 0;
            case "!=" -> actual.compareTo(expected) != 0;
            default -> throw new SilentException("不支持的比较操作符: " + operator);
        };
    }

    /**
     * 生成期望比较说明
     */
    private String expectedCompare(JSONObject config, String defaultText) {
        if (config.containsKey("min") || config.containsKey("max")) {
            BigDecimal min = config.getBigDecimal("min");
            BigDecimal max = config.getBigDecimal("max");
            if (min != null && max != null) {
                return min + " - " + max;
            }
            if (min != null) {
                return ">= " + min;
            }
            if (max != null) {
                return "<= " + max;
            }
        }
        String operator = config.getString("operator");
        BigDecimal expected = config.getBigDecimal("expected");
        if (hasText(operator) && expected != null) {
            return operator + " " + expected;
        }
        return defaultText;
    }

    /**
     * 转换BigDecimal值
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number) {
            return new BigDecimal(String.valueOf(value));
        }
        return new BigDecimal(String.valueOf(value));
    }

    /**
     * 格式化小数
     */
    private String decimalText(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /**
     * 获取表字段
     */
    private List<ColumnValObj> columns(MetadataTable table) {
        return Optional.ofNullable(table.getTable().getColumns()).orElse(List.of());
    }

    /**
     * 判断字段是否存在
     */
    private boolean hasColumn(MetadataTable table, String columnName) {
        return columns(table).stream().anyMatch(column -> Objects.equals(column.getName(), columnName));
    }

    /**
     * 拼接表引用
     */
    private String tableRef(MetadataTable table) {
        return quoteIdentifier(table.getTable().getCatalog()) + "." + quoteIdentifier(table.getTable().getSchema()) + "." + quoteIdentifier(table.getName());
    }

    /**
     * 拼接字段引用
     */
    private String columnRef(String columnName) {
        return quoteIdentifier(columnName);
    }

    /**
     * 引用标识符
     */
    private String quoteIdentifier(String identifier) {
        Assert.isTrue(IDENTIFIER_PATTERN.matcher(identifier).matches(), new SilentException("非法标识符: " + identifier));
        return "`" + identifier + "`";
    }

    /**
     * 生成过滤条件
     */
    private String where(MetadataQualityRule rule) {
        return hasText(rule.getFilterSql()) ? " where (" + rule.getFilterSql() + ")" : "";
    }

    /**
     * 追加过滤条件
     */
    private String appendCondition(MetadataQualityRule rule, String condition) {
        if (hasText(rule.getFilterSql())) {
            return " where (" + rule.getFilterSql() + ") and (" + condition + ")";
        }
        return " where " + condition;
    }

    /**
     * 转换期望区间
     */
    private String expectedRange(Long minCount, Long maxCount) {
        if (minCount != null && maxCount != null) {
            return minCount + " - " + maxCount;
        }
        if (minCount != null) {
            return ">= " + minCount;
        }
        if (maxCount != null) {
            return "<= " + maxCount;
        }
        return "任意";
    }

    /**
     * 生成规则去重键
     */
    private String ruleKey(String type, String columnName) {
        return normalizeRuleType(type) + ":" + Optional.ofNullable(columnName).orElse("");
    }

    /**
     * 类型标准化
     */
    private String normalizeType(String type) {
        return Optional.ofNullable(type).orElse("").toLowerCase(Locale.ROOT);
    }

    /**
     * 判断是否日期时间类型
     */
    private boolean isDateTimeType(String type) {
        return type.contains("date") || type.contains("time");
    }

    /**
     * 判断是否数值类型
     */
    private boolean isNumericType(String type) {
        return type.contains("int") || type.contains("decimal") || type.contains("numeric")
                || type.contains("double") || type.contains("float") || type.contains("number");
    }

    /**
     * SQL字符串转义
     */
    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    /**
     * SQL字面量
     */
    private String sqlLiteral(Object value) {
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        return "'" + escapeSql(String.valueOf(value)) + "'";
    }

    /**
     * 判断文本是否有内容
     */
    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
