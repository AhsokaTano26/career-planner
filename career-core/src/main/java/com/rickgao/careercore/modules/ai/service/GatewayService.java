package com.rickgao.careercore.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.rickgao.careercore.modules.ai.dto.ChatCompletionRequest;
import com.rickgao.careercore.modules.ai.dto.GatewayGenerateRequest;
import com.rickgao.careercore.modules.ai.dto.GatewayMessage;
import com.rickgao.careercore.modules.ai.vo.GatewayGenerateResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 网关高阶文本生成服务（POST /api/v1/gateway/generate）。
 *
 * <p>组装 messages、调 {@link LlmGateway#generateDetailed} 取得文本与 token 用量，
 * 自行计时（durationMs）并生成 requestId。
 *
 * <p>Demo 精简点 / 后续迭代替换位置:
 *  - requestId 当前用 UUID（短 32 位），未与链路追踪 traceId 绑定，后续若接入全链路追踪应替换为 TraceIdUtil；
 *  - scene/userRef/promptVersion 仅记录日志，不持久化也不做限流。
 */
@Service
public class GatewayService {

    private static final String DISCLAIMER = "智能生成，供探索参考";

    private final LlmGateway llm;

    public GatewayService(LlmGateway llm) {
        this.llm = llm;
    }

    public GatewayGenerateResponse generate(GatewayGenerateRequest req) {
        double temperature = req.getTemperature() != null ? req.getTemperature() : 0.7;
        int maxTokens = req.getMaxTokens() != null ? req.getMaxTokens() : 500;
        List<Map<String, String>> messages = toLlmMessages(req.getMessages());

        long start = System.nanoTime();
        LlmGateway.GenerateDetailedResult result = llm.generateDetailed(
                messages, temperature, maxTokens, req.getModelGroup());
        long durationMs = (System.nanoTime() - start) / 1_000_000L;

        return GatewayGenerateResponse.builder()
                .text(result.text())
                .model(result.model())
                .requestId(UUID.randomUUID().toString().replace("-", ""))
                .durationMs(durationMs)
                .totalTokens(result.totalTokens())
                .disclaimer(DISCLAIMER)
                .build();
    }

    /** DTO 列表（{role, content}）转 LlmGateway 期望的 Map 列表。 */
    private List<Map<String, String>> toLlmMessages(List<GatewayMessage> input) {
        if (input == null) {
            return List.of();
        }
        List<Map<String, String>> out = new ArrayList<>(input.size());
        for (GatewayMessage m : input) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("role", m.getRole());
            entry.put("content", m.getContent());
            out.add(entry);
        }
        return out;
    }

    /**
     * OpenAI 兼容聊天补全（POST /api/v1/gateway/chat/completions）：
     * 透传到 career-ai /v1/chat/completions，原样返回完整响应。
     *
     * <p>Demo 精简点 / 后续迭代替换位置:
     *  - 不解析 choices，由调用方（Controller）按 OpenAI 标准直接序列化；
     *  - 不做幂等/限流/审计，靠 career-ai 网关侧 ai_call_log 与限流。
     */
    public JsonNode chatCompletions(ChatCompletionRequest req) {
        List<Map<String, String>> messages = toLlmMessages(req.getMessages());
        return llm.chatCompletionsRaw(
                messages,
                req.getTemperature(),
                req.getMaxTokens(),
                req.getStream(),
                req.getUser(),
                req.getModel());
    }
}
