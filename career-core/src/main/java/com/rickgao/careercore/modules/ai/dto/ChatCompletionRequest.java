package com.rickgao.careercore.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容聊天补全请求（POST /api/v1/gateway/chat/completions）。
 * 入参结构与 OpenAI ChatCompletionRequest 对齐，由 career-core 透传至 career-ai 网关。
 *
 * <p>Demo 精简点 / 后续迭代替换位置:
 *  - stream 字段当前在 Service 层校验必须为 null/false（career-ai 不支持 SSE 流式响应）；
 *  - tools/logprobs/n 等扩展参数按 career-ai 策略静默忽略，后续如需校验应在网关侧补齐。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    /** 网关模型组名（如 default）；缺省走 default 组。 */
    private String model;

    @NotEmpty(message = "messages 不能为空")
    @Valid
    private List<GatewayMessage> messages;

    private Double temperature;

    private Integer maxTokens;

    /** 是否流式输出。当前 career-ai 不支持 stream=true，会由 Service 显式拒绝。 */
    private Boolean stream;

    /** 脱敏用户引用（透传网关写入 ai_call_log.user_ref）。 */
    private String user;
}
