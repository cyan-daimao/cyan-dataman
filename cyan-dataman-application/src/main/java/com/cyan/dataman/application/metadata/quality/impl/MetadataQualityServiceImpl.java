package com.cyan.dataman.application.metadata.quality.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.dataman.application.metadata.quality.MetadataQualityService;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityAlertBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityResultBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleBO;
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

    public MetadataQualityServiceImpl(MetadataTableRepository metadataTableRepository,
                                      MetadataQualityRuleRepository ruleRepository,
                                      MetadataQualityRunRepository runRepository,
                                      MetadataQualityResultRepository resultRepository,
                                      MetadataQualityAlertRepository alertRepository,
                                      StarRocksUtil starRocksUtil) {
        this.metadataTableRepository = metadataTableRepository;
        this.ruleRepository = ruleRepository;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.alertRepository = alertRepository;
        this.starRocksUtil = starRocksUtil;
    }

    /**
     * 查询规则模板
     */
    @Override
    public List<MetadataQualityRuleTemplateBO> listRuleTemplates() {
        return List.of(
                template("ROW_COUNT", "完整性", "表行数检查", "检查表行数是否在预期范围内", false, "{\"minCount\":1}"),
                template("FRESHNESS", "及时性", "数据及时性检查", "检查时间字段距离当前时间是否超出阈值", true, "{\"maxDelayMinutes\":1440}"),
                template("CUSTOM_SQL", "准确性", "自定义SQL检查", "执行返回 fail_count 的自定义SQL", false, "{\"sql\":\"select 0 as fail_count, count(1) as total_count from table\"}"),
                template("NOT_NULL", "完整性", "字段非空检查", "检查字段空值数量", true, "{}"),
                template("UNIQUE", "唯一性", "字段唯一检查", "检查字段重复数量", true, "{}"),
                template("ENUM", "有效性", "枚举值检查", "检查字段值是否在枚举范围内", true, "{\"values\":[\"A\",\"B\"]}"),
                template("RANGE", "有效性", "范围检查", "检查字段值是否在上下限范围内", true, "{\"min\":0,\"max\":100}")
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
        Set<String> existingKeys = new HashSet<>();
        existingRules.forEach(rule -> existingKeys.add(ruleKey(rule.getRuleType(), rule.getColumnName())));

        List<MetadataQualityRule> createdRules = new ArrayList<>();
        for (ColumnValObj column : columns(table)) {
            String type = normalizeType(column.getType());
            if (Boolean.FALSE.equals(column.getNullable())) {
                addRecommendedRule(tableId, createdRules, existingKeys, "NOT_NULL", "完整性",
                        column.getName(), column.getName() + " 非空检查", "FAIL", "{}");
            }
            if (isDateTimeType(type)) {
                addRecommendedRule(tableId, createdRules, existingKeys, "FRESHNESS", "及时性",
                        column.getName(), column.getName() + " 及时性检查", "WARN", "{\"maxDelayMinutes\":1440}");
            }
        }

        for (IndexValObj index : Optional.ofNullable(table.getTable().getIndexes()).orElse(List.of())) {
            String indexType = Optional.ofNullable(index.getIndexType()).orElse("").toUpperCase(Locale.ROOT);
            if ((indexType.contains("UNIQUE") || indexType.contains("PRIMARY"))
                    && index.getFieldNames() != null && index.getFieldNames().size() == 1) {
                String columnName = index.getFieldNames().get(0);
                if (hasColumn(table, columnName)) {
                    addRecommendedRule(tableId, createdRules, existingKeys, "UNIQUE", "唯一性",
                            columnName, columnName + " 唯一性检查", "FAIL", "{}");
                }
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
            return switch (rule.getRuleType()) {
                case "ROW_COUNT" -> executeRowCount(table, rule, result);
                case "NOT_NULL" -> executeNotNull(table, rule, result);
                case "UNIQUE" -> executeUnique(table, rule, result);
                case "ENUM" -> executeEnum(table, rule, result);
                case "RANGE" -> executeRange(table, rule, result);
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
    private MetadataQualityResult executeRowCount(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        JSONObject config = config(rule);
        Long minCount = config.getLong("minCount");
        Long maxCount = config.getLong("maxCount");
        long total = firstLong(query("select count(1) as total_count from " + tableRef(table) + where(rule)), "total_count");
        boolean passed = (minCount == null || total >= minCount) && (maxCount == null || total <= maxCount);
        return finish(result, rule, passed, total, passed ? 0L : 1L,
                String.valueOf(total), expectedRange(minCount, maxCount), null);
    }

    /**
     * 执行非空规则
     */
    private MetadataQualityResult executeNotNull(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        String column = columnRef(rule.getColumnName());
        String sql = "select count(1) as total_count, sum(case when " + column + " is null then 1 else 0 end) as fail_count from "
                + tableRef(table) + where(rule);
        Map<String, Object> row = firstRow(query(sql));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        return finish(result, rule, fail == 0, total, fail, String.valueOf(fail), "0",
                "select * from " + tableRef(table) + appendCondition(rule, column + " is null") + " limit 100");
    }

    /**
     * 执行唯一性规则
     */
    private MetadataQualityResult executeUnique(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        String column = columnRef(rule.getColumnName());
        String sql = "select count(1) as total_count, count(1) - count(distinct " + column + ") as fail_count from "
                + tableRef(table) + where(rule);
        Map<String, Object> row = firstRow(query(sql));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        return finish(result, rule, fail == 0, total, fail, String.valueOf(fail), "0",
                "select " + column + ", count(1) as duplicate_count from " + tableRef(table)
                        + where(rule) + " group by " + column + " having count(1) > 1 limit 100");
    }

    /**
     * 执行枚举规则
     */
    private MetadataQualityResult executeEnum(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        JSONArray values = config(rule).getJSONArray("values");
        Assert.isTrue(values != null && !values.isEmpty(), new SilentException("ENUM 规则 values 不能为空"));
        String inValues = values.stream().map(value -> "'" + escapeSql(String.valueOf(value)) + "'").reduce((a, b) -> a + "," + b).orElse("''");
        String column = columnRef(rule.getColumnName());
        String invalid = column + " is not null and cast(" + column + " as varchar) not in (" + inValues + ")";
        Map<String, Object> row = firstRow(query("select count(1) as total_count, sum(case when " + invalid
                + " then 1 else 0 end) as fail_count from " + tableRef(table) + where(rule)));
        long total = toLong(row.get("total_count"));
        long fail = toLong(row.get("fail_count"));
        return finish(result, rule, fail == 0, total, fail, String.valueOf(fail), values.toJSONString(),
                "select * from " + tableRef(table) + appendCondition(rule, invalid) + " limit 100");
    }

    /**
     * 执行范围规则
     */
    private MetadataQualityResult executeRange(MetadataTable table, MetadataQualityRule rule, MetadataQualityResult result) throws SQLException {
        JSONObject config = config(rule);
        String column = columnRef(rule.getColumnName());
        List<String> conditions = new ArrayList<>();
        if (config.containsKey("min")) {
            conditions.add(column + " < " + sqlLiteral(config.get("min")));
        }
        if (config.containsKey("max")) {
            conditions.add(column + " > " + sqlLiteral(config.get("max")));
        }
        Assert.isTrue(!conditions.isEmpty(), new SilentException("RANGE 规则 min/max 不能同时为空"));
        String invalid = column + " is not null and (" + String.join(" or ", conditions) + ")";
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
        if (List.of("NOT_NULL", "UNIQUE", "ENUM", "RANGE", "FRESHNESS").contains(cmd.getRuleType())) {
            Assert.notBlank(cmd.getColumnName(), new SilentException(cmd.getRuleType() + " 规则字段不能为空"));
            Assert.isTrue(hasColumn(table, cmd.getColumnName()), new SilentException("字段不存在: " + cmd.getColumnName()));
        }
        if (hasText(cmd.getConfigJson())) {
            try {
                JSON.parseObject(cmd.getConfigJson());
            } catch (Exception e) {
                throw new SilentException("规则配置JSON格式不正确");
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
        if (List.of("NOT_NULL", "UNIQUE", "ENUM", "RANGE", "FRESHNESS").contains(rule.getRuleType())) {
            Assert.isTrue(hasColumn(table, rule.getColumnName()), new SilentException("字段不存在: " + rule.getColumnName()));
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
        return type + ":" + Optional.ofNullable(columnName).orElse("");
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
