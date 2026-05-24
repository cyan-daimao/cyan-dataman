package com.cyan.dataman.adapter.metadata.http;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.cyan.arch.common.api.Response;
import com.cyan.dataman.adapter.metadata.http.convert.AiRelationSuggestAdapterConvert;
import com.cyan.dataman.adapter.metadata.http.dto.AiRelationSuggestRequestDTO;
import com.cyan.dataman.adapter.metadata.http.dto.AiRelationSuggestionDTO;
import com.cyan.dataman.application.metadata.AiRelationSuggestService;
import com.cyan.dataman.application.metadata.AiRelationSuggestStreamListener;
import com.cyan.dataman.application.metadata.bo.AiRelationSuggestionBO;
import com.cyan.dataman.client.table.dto.JoinPathsRequestDTO;
import com.cyan.dataman.client.table.dto.TableRelationDTO;
import com.cyan.dataman.client.table.dto.TableRelationsResultDTO;
import com.cyan.dataman.application.metadata.TableRelationService;
import com.cyan.dataman.application.metadata.cmd.CreateRelationCmd;
import com.cyan.employee.login.filter.UserContextHolder;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 表关系控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/metadata/tables")
public class TableRelationController {

    private static final MediaType SSE_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final TableRelationService tableRelationService;
    private final AiRelationSuggestService aiRelationSuggestService;

    public TableRelationController(TableRelationService tableRelationService,
                                   AiRelationSuggestService aiRelationSuggestService) {
        this.tableRelationService = tableRelationService;
        this.aiRelationSuggestService = aiRelationSuggestService;
    }

    /**
     * 获取表的所有关联关系
     *
     * @param catalog 表 catalog
     * @param schema  表 schema
     * @param table   表名
     * @return 出向和入向关联关系
     */
    @GetMapping("/{catalog}/{schema}/{table}/relations")
    public Response<TableRelationsResultDTO> getTableRelations(
            @PathVariable String catalog,
            @PathVariable String schema,
            @PathVariable String table) {
        Map<String, List<TableRelationDTO>> relations = tableRelationService.getTableRelations(catalog, schema, table);
        TableRelationsResultDTO result = new TableRelationsResultDTO()
                .setOutgoing(relations.get("outgoing"))
                .setIncoming(relations.get("incoming"));
        return Response.success(result);
    }

    /**
     * 创建关联关系
     *
     * @param cmd 创建命令
     * @return 创建后的关联关系
     */
    @PostMapping("/relations")
    public Response<TableRelationDTO> createRelation(@RequestBody @Valid CreateRelationCmd cmd) {
        String createdBy = UserContextHolder.getCurrentEmployee().getPassport();
        TableRelationDTO relation = tableRelationService.createRelation(cmd, createdBy);
        return Response.success(relation);
    }

    /**
     * AI 推荐关联关系
     *
     * @param request 推荐请求
     * @return AI 推荐候选列表
     */
    @PostMapping("/relations/ai-suggest")
    public Response<List<AiRelationSuggestionDTO>> suggestRelations(@RequestBody @Valid AiRelationSuggestRequestDTO request) {
        List<AiRelationSuggestionBO> suggestions = aiRelationSuggestService.suggest(
                request.getCatalog(), request.getSchema(), request.getTable(), request.getMaxCandidates());
        return Response.success(AiRelationSuggestAdapterConvert.INSTANCE.toAiRelationSuggestionDTOList(suggestions));
    }

    /**
     * AI 流式推荐关联关系
     *
     * @param request 推荐请求
     * @return SSE 响应
     */
    @PostMapping(value = "/relations/ai-suggest/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter suggestRelationsStream(@RequestBody @Valid AiRelationSuggestRequestDTO request) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> {
            try {
                aiRelationSuggestService.suggestStream(
                        request.getCatalog(),
                        request.getSchema(),
                        request.getTable(),
                        request.getMaxCandidates(),
                        new AiRelationSuggestStreamEmitter(emitter)
                );
                sendEvent(emitter, "done", new JSONObject().fluentPut("message", "AI 推荐完成"));
                emitter.complete();
            } catch (Exception e) {
                sendEvent(emitter, "error", new JSONObject().fluentPut("message", e.getMessage()));
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * SSE 推荐监听器
     */
    private static class AiRelationSuggestStreamEmitter implements AiRelationSuggestStreamListener {
        private final SseEmitter emitter;

        private AiRelationSuggestStreamEmitter(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * 输出状态信息
         */
        @Override
        public void onStatus(String message) {
            sendEvent(emitter, "status", new JSONObject().fluentPut("message", message));
        }

        /**
         * 输出 AI 原始回复片段
         */
        @Override
        public void onAnswer(String content) {
            sendEvent(emitter, "answer", new JSONObject().fluentPut("content", content));
        }

        /**
         * 输出推荐结果
         */
        @Override
        public void onResult(List<AiRelationSuggestionBO> suggestions) {
            List<AiRelationSuggestionDTO> dtos = AiRelationSuggestAdapterConvert.INSTANCE.toAiRelationSuggestionDTOList(suggestions);
            sendEvent(emitter, "result", new JSONObject().fluentPut("data", dtos == null ? Collections.emptyList() : dtos));
        }
    }

    /**
     * 发送 SSE 事件
     */
    private static void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(JSON.toJSONString(data), SSE_JSON_UTF8));
        } catch (IOException ignored) {
            emitter.complete();
        }
    }

    /**
     * 删除关联关系
     *
     * @param id 关联ID
     * @return 空响应
     */
    @DeleteMapping("/relations/{id}")
    public Response<Void> deleteRelation(@PathVariable Long id) {
        tableRelationService.deleteRelation(id);
        return Response.success();
    }

    /**
     * 批量获取多张表的 JOIN 路径（供指标平台调用）
     *
     * @param request 请求体
     * @return JOIN 路径列表
     */
    @PostMapping("/relations/join-paths")
    public Response<List<TableRelationDTO>> findJoinPaths(@RequestBody JoinPathsRequestDTO request) {
        JoinPathsRequestDTO.TableRefDTO fact = request.getFactTable();
        List<JoinPathsRequestDTO.TableRefDTO> dims = request.getDimensionTables();

        List<String[]> dimTables = dims == null ? List.of() : dims.stream()
                .map(d -> new String[]{d.getCatalog(), d.getSchema(), d.getTable()})
                .toList();

        List<TableRelationDTO> paths = tableRelationService.findJoinPaths(
                fact.getCatalog(), fact.getSchema(), fact.getTable(), dimTables);
        return Response.success(paths);
    }
}
