package com.cyan.dataman.infra.rpc.dify.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Dify 对话响应
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DifyChatMessageResponse {

    /**
     * 事件类型
     */
    private String event;

    /**
     * 任务ID
     */
    @JsonProperty("task_id")
    private String taskId;

    /**
     * 消息ID
     */
    @JsonProperty("message_id")
    private String messageId;

    /**
     * 对话ID
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * 回复内容
     */
    private String answer;
}
