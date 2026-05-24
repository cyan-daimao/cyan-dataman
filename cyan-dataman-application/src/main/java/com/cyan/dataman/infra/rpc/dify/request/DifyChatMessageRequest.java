package com.cyan.dataman.infra.rpc.dify.request;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Dify 对话请求
 *
 * @author cy.Y
 * @since 1.0.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class DifyChatMessageRequest {

    /**
     * 应用输入变量
     */
    private JSONObject inputs;

    /**
     * 用户问题
     */
    private String query;

    /**
     * 响应模式
     */
    @JsonProperty("response_mode")
    private String responseMode;

    /**
     * 用户标识
     */
    private String user;

    /**
     * 文件列表
     */
    private List<Object> files;
}
