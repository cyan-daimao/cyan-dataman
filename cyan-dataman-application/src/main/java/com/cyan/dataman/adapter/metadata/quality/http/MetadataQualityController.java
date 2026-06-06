package com.cyan.dataman.adapter.metadata.quality.http;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.quality.http.convert.MetadataQualityAdapterConvert;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityAlertDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRuleDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRuleRequestDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRuleSuggestionDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRuleTemplateDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualityRunDTO;
import com.cyan.dataman.adapter.metadata.quality.http.dto.MetadataQualitySummaryDTO;
import com.cyan.dataman.application.metadata.quality.MetadataQualityService;
import com.cyan.dataman.application.metadata.quality.QualityRuleRecommendStreamListener;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityAlertBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRuleSuggestionBO;
import com.cyan.dataman.application.metadata.quality.bo.MetadataQualityRunBO;
import com.cyan.employee.login.filter.UserContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 元数据质量接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata")
public class MetadataQualityController {

    private static final MediaType TEXT_EVENT_STREAM_UTF8 = new MediaType("text", "event-stream", StandardCharsets.UTF_8);

    private final MetadataQualityService metadataQualityService;

    public MetadataQualityController(MetadataQualityService metadataQualityService) {
        this.metadataQualityService = metadataQualityService;
    }

    /**
     * 查询规则模板
     */
    @GetMapping("/quality/rule-templates")
    public Response<List<MetadataQualityRuleTemplateDTO>> listRuleTemplates() {
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toRuleTemplateDTOList(metadataQualityService.listRuleTemplates()));
    }

    /**
     * 查询质量汇总
     */
    @GetMapping("/tables/{tableId}/quality/summary")
    public Response<MetadataQualitySummaryDTO> getSummary(@PathVariable String tableId) {
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toSummaryDTO(metadataQualityService.getSummary(tableId)));
    }

    /**
     * 查询表规则
     */
    @GetMapping("/tables/{tableId}/quality/rules")
    public Response<List<MetadataQualityRuleDTO>> listRules(@PathVariable String tableId) {
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toRuleDTOList(metadataQualityService.listRules(tableId)));
    }

    /**
     * 创建规则
     */
    @PostMapping("/tables/{tableId}/quality/rules")
    public Response<MetadataQualityRuleDTO> createRule(@PathVariable String tableId,
                                                       @RequestBody MetadataQualityRuleRequestDTO dto) {
        MetadataQualityRuleBO rule = metadataQualityService.createRule(tableId, MetadataQualityAdapterConvert.INSTANCE.toRuleCmd(dto));
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toRuleDTO(rule));
    }

    /**
     * 更新规则
     */
    @PutMapping("/tables/{tableId}/quality/rules/{ruleId}")
    public Response<MetadataQualityRuleDTO> updateRule(@PathVariable String tableId,
                                                       @PathVariable String ruleId,
                                                       @RequestBody MetadataQualityRuleRequestDTO dto) {
        MetadataQualityRuleBO rule = metadataQualityService.updateRule(tableId, ruleId, MetadataQualityAdapterConvert.INSTANCE.toRuleCmd(dto));
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toRuleDTO(rule));
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/tables/{tableId}/quality/rules/{ruleId}")
    public Response<Void> deleteRule(@PathVariable String tableId, @PathVariable String ruleId) {
        metadataQualityService.deleteRule(tableId, ruleId);
        return Response.success();
    }

    /**
     * 推荐规则
     */
    @PostMapping("/tables/{tableId}/quality/rules/recommend")
    public Response<List<MetadataQualityRuleDTO>> recommendRules(@PathVariable String tableId) {
        List<MetadataQualityRuleBO> rules = metadataQualityService.recommendRules(tableId);
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toRuleDTOList(rules));
    }

    /**
     * AI 流式推荐规则候选
     */
    @PostMapping(value = "/tables/{tableId}/quality/rules/recommend/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> recommendRulesStream(@PathVariable String tableId) {
        StreamingResponseBody body = outputStream -> {
            QualityRuleRecommendStreamWriter writer = new QualityRuleRecommendStreamWriter(outputStream);
            try {
                metadataQualityService.recommendRulesStream(tableId, writer);
                sendEvent(outputStream, "done", new JSONObject().fluentPut("message", "AI 推荐完成"));
            } catch (Exception e) {
                sendEvent(outputStream, "error", new JSONObject().fluentPut("message", e.getMessage()));
            }
        };
        return ResponseEntity.ok()
                .contentType(TEXT_EVENT_STREAM_UTF8)
                .body(body);
    }

    /**
     * 立即运行质量检查
     */
    @PostMapping("/tables/{tableId}/quality/runs")
    public Response<MetadataQualityRunDTO> run(@PathVariable String tableId) {
        MetadataQualityRunBO run = metadataQualityService.run(tableId);
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toRunDTO(run));
    }

    /**
     * 查询运行列表
     */
    @GetMapping("/tables/{tableId}/quality/runs")
    public Response<List<MetadataQualityRunDTO>> listRuns(@PathVariable String tableId) {
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toRunDTOList(metadataQualityService.listRuns(tableId)));
    }

    /**
     * 查询运行详情
     */
    @GetMapping("/quality/runs/{runId}")
    public Response<MetadataQualityRunDTO> getRunDetail(@PathVariable String runId) {
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toRunDTO(metadataQualityService.getRunDetail(runId)));
    }

    /**
     * 查询表告警
     */
    @GetMapping("/tables/{tableId}/quality/alerts")
    public Response<List<MetadataQualityAlertDTO>> listAlerts(@PathVariable String tableId) {
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toAlertDTOList(metadataQualityService.listAlerts(tableId)));
    }

    /**
     * 关闭告警
     */
    @PostMapping("/quality/alerts/{id}/close")
    public Response<MetadataQualityAlertDTO> closeAlert(@PathVariable String id) {
        String operator = UserContextHolder.getCurrentEmployee().getPassport();
        MetadataQualityAlertBO alert = metadataQualityService.closeAlert(id, operator);
        return Response.success(MetadataQualityAdapterConvert.INSTANCE.toAlertDTO(alert));
    }

    /**
     * 数据质量推荐SSE写入器
     */
    private static class QualityRuleRecommendStreamWriter implements QualityRuleRecommendStreamListener {
        private final OutputStream outputStream;

        private QualityRuleRecommendStreamWriter(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        /**
         * 输出状态信息
         */
        @Override
        public void onStatus(String message) {
            sendEvent(outputStream, "status", new JSONObject().fluentPut("message", message));
        }

        /**
         * 输出 AI 原始回复片段
         */
        @Override
        public void onAnswer(String content) {
            sendEvent(outputStream, "answer", new JSONObject().fluentPut("content", content));
        }

        /**
         * 输出推荐候选
         */
        @Override
        public void onSuggestions(List<MetadataQualityRuleSuggestionBO> suggestions) {
            List<MetadataQualityRuleSuggestionDTO> dtos = MetadataQualityAdapterConvert.INSTANCE.toRuleSuggestionDTOList(suggestions);
            sendEvent(outputStream, "suggestions", new JSONObject().fluentPut("data", dtos == null ? Collections.emptyList() : dtos));
        }
    }

    /**
     * 发送 SSE 事件
     */
    private static void sendEvent(OutputStream outputStream, String eventName, Object data) {
        try {
            String payload = "event:" + eventName + "\n"
                    + "data:" + JSON.toJSONString(data) + "\n\n";
            outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            throw new IllegalStateException("SSE 事件发送失败", e);
        }
    }
}
