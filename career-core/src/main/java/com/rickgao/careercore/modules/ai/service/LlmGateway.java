package com.rickgao.careercore.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 大模型网关客户端：career-core 全量大模型调用统一走 career-ai AI 网关
 * （{@code POST /v1/chat/completions}，OpenAI 兼容，非流式）。
 *
 * <p>配置（环境变量注入，不硬编码）：AI_GATEWAY_BASE_URL（可选，默认 http://127.0.0.1:8000）、
 * AI_GATEWAY_API_KEY（可选，网关内部鉴权密钥）、AI_GATEWAY_MODEL（可选，网关模型组名，默认 default）、
 * AI_GATEWAY_TIMEOUT（可选，秒，默认 30）。
 *
 * <p>替换点已完成（2026-08-29，AI 网关计划 v2）：原直连 DeepSeek 实现已移除；多渠道路由、
 * 负载均衡、失败重试、整组降级由 career-ai 网关（gateway/*，基于 LiteLLM）负责。
 *
 * <p>2026-09 Phase 2：career-core 在调用大模型后写入 ai_call_log（之前仅 career-ai 写）；
 * fail-open，日志写失败不影响业务。
 *
 * <p>Demo 精简点 / 后续迭代替换位置：generate() 调用方传 scene/userRef，gateway 自身仍按
 * scene=gateway_api 记录（透传由后续迭代做）。
 */
@Component
public class LlmGateway {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8000";
    private static final String DEFAULT_MODEL_GROUP = "default";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiCallLogWriter callLogWriter;
    private final String apiKey;
    private final String model;

    public LlmGateway(
            @Value("${ai.gateway-api-key:}") String apiKey,
            @Value("${ai.gateway-base-url:}") String baseUrl,
            @Value("${ai.gateway-model:}") String model,
            @Value("${ai.gateway-timeout:30}") int timeout,
            ObjectMapper objectMapper,
            AiCallLogWriter callLogWriter) {
        this.apiKey = apiKey;
        String effectiveBaseUrl = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL_GROUP : model;
        this.objectMapper = objectMapper;
        this.callLogWriter = callLogWriter;
        this.restClient = RestClient.builder()
                .baseUrl(effectiveBaseUrl.replaceAll("/+$", ""))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(RestClientFactory.factory(timeout))
                .build();
    }

    /**
     * 调用网关 OpenAI 兼容 chat/completions，返回模型回复文本。
     * Phase 2：调用前后写入 ai_call_log（scene、userRef 由调用方传入）。
     *
     * @throws BizException 网络失败 / 鉴权失败 / HTTP 错误 / 返回结构异常时抛出（code=INTERNAL_ERROR）。
     */
    public String generate(List<Map<String, String>> messages, double temperature, int maxTokens) {
        return generate(messages, temperature, maxTokens, "gateway_api", null, null, null);
    }

    /** 带 ai_call_log 元数据的 generate 重载（推荐使用）。 */
    public String generate(List<Map<String, String>> messages, double temperature, int maxTokens,
                           String scene, String userRef, String promptVersion, String requestId) {
        long startMs = System.currentTimeMillis();
        String effectiveRequestId = requestId == null || requestId.isBlank()
                ? java.util.UUID.randomUUID().toString().replace("-", "") : requestId;
        String requestHash = sha256Short(messages);
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens);
        String body;
        try {
            var request = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("X-Request-Id", effectiveRequestId)
                    .body(payload);
            if (apiKey != null && !apiKey.isBlank()) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }
            body = request.retrieve().body(String.class);
        } catch (Exception exc) {
            writeCallLog(effectiveRequestId, scene, userRef, promptVersion, model,
                    System.currentTimeMillis() - startMs, "FAILED", null, requestHash, null);
            throw new BizException(ResultCode.INTERNAL_ERROR, "AI 网关调用失败（model=" + model + "）：" + exc.getMessage());
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String text = root.path("choices").path(0).path("message").path("content").asText("").trim();
            Integer tokens = null;
            JsonNode usage = root.path("usage");
            if (usage.isObject() && usage.has("total_tokens")) {
                tokens = usage.path("total_tokens").asInt(0);
            }
            String resolvedModel = root.path("model").asText(model);
            writeCallLog(effectiveRequestId, scene, userRef, promptVersion, resolvedModel,
                    System.currentTimeMillis() - startMs, "SUCCESS", tokens, requestHash, sha256Short(text));
            return text;
        } catch (Exception exc) {
            writeCallLog(effectiveRequestId, scene, userRef, promptVersion, model,
                    System.currentTimeMillis() - startMs, "FAILED", null, requestHash, null);
            throw new BizException(ResultCode.INTERNAL_ERROR, "AI 网关返回结构异常：" + body);
        }
    }

    /**
     * 网关高阶文本生成（POST /api/v1/gateway/generate 内部实现）：返回含文本/模型/usage 的原始解析树。
     *
     * <p>与 {@link #generate(List, double, int)} 区别：保留网关 usage.total_tokens 等元数据，
     * 由调用方按需取用。原 {@code generate()} 行为不变，保持现有 AiService 调用方兼容。
     *
     * <p>Demo 精简点 / 后续迭代替换位置:modelGroup/userRef/promptVersion 当前仅做日志识别，
     * 若需按用户归因或 prompt 版本路由，应在网关层或本方法签名层补充对应字段。
     *
     * @throws BizException 网络失败 / 鉴权失败 / HTTP 错误 / 返回结构异常时抛出（code=INTERNAL_ERROR）。
     */
    public GenerateDetailedResult generateDetailed(List<Map<String, String>> messages,
                                                    double temperature,
                                                    int maxTokens,
                                                    String modelGroup) {
        return generateDetailed(messages, temperature, maxTokens, modelGroup, "gateway_api", null, null, null);
    }

    /** 带 ai_call_log 元数据的 generateDetailed 重载。 */
    public GenerateDetailedResult generateDetailed(List<Map<String, String>> messages,
                                                    double temperature,
                                                    int maxTokens,
                                                    String modelGroup,
                                                    String scene, String userRef,
                                                    String promptVersion, String requestId) {
        String effectiveModel = (modelGroup == null || modelGroup.isBlank()) ? model : modelGroup;
        long startMs = System.currentTimeMillis();
        String effectiveRequestId = requestId == null || requestId.isBlank()
                ? java.util.UUID.randomUUID().toString().replace("-", "") : requestId;
        String requestHash = sha256Short(messages);
        Map<String, Object> payload = Map.of(
                "model", effectiveModel,
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens);
        String body;
        try {
            var request = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("X-Request-Id", effectiveRequestId)
                    .body(payload);
            if (apiKey != null && !apiKey.isBlank()) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }
            body = request.retrieve().body(String.class);
        } catch (Exception exc) {
            writeCallLog(effectiveRequestId, scene, userRef, promptVersion, effectiveModel,
                    System.currentTimeMillis() - startMs, "FAILED", null, requestHash, null);
            throw new BizException(ResultCode.INTERNAL_ERROR,
                    "AI 网关调用失败（model=" + effectiveModel + "）：" + exc.getMessage());
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String text = root.path("choices").path(0).path("message").path("content").asText("").trim();
            String resolvedModel = root.path("model").asText(effectiveModel);
            Integer totalTokens = null;
            JsonNode usage = root.path("usage");
            if (usage.isObject() && usage.has("total_tokens")) {
                totalTokens = usage.path("total_tokens").asInt(0);
            }
            writeCallLog(effectiveRequestId, scene, userRef, promptVersion, resolvedModel,
                    System.currentTimeMillis() - startMs, "SUCCESS", totalTokens, requestHash, sha256Short(text));
            return new GenerateDetailedResult(text, resolvedModel, totalTokens);
        } catch (Exception exc) {
            writeCallLog(effectiveRequestId, scene, userRef, promptVersion, effectiveModel,
                    System.currentTimeMillis() - startMs, "FAILED", null, requestHash, null);
            throw new BizException(ResultCode.INTERNAL_ERROR, "AI 网关返回结构异常：" + body);
        }
    }

    public String getModel() {
        return model;
    }

    // ---------------------------------------------------------------- ai_call_log 写入（委托给独立 bean，REQUIRES_NEW 保证不被外层回滚）

    private void writeCallLog(String requestId, String scene, String userRef, String promptVersion,
                              String modelName, long durationMs, String status,
                              Integer tokenEstimate, String requestHash, String inputHash) {
        callLogWriter.write(requestId, scene, userRef, promptVersion, modelName, durationMs,
                status, tokenEstimate, requestHash, inputHash);
    }

    private String sha256Short(Object value) {
        return callLogWriter.sha256Short(value);
    }

    /**
     * 网关生成结果（文本 + 模型 + token 用量）。
     * Demo 精简点 / 后续迭代替换位置:目前不返回 prompt_tokens/completion_tokens 细分，
     * 后续若需用量计费可在此补充字段。
     */
    public record GenerateDetailedResult(String text, String model, Integer totalTokens) {
    }

    /**
     * OpenAI 兼容聊天补全透传（POST /api/v1/gateway/chat/completions 内部实现）：
     * 把网关完整响应（id/object/created/model/choices/usage）以原始 JsonNode 形式返回，
     * 由调用方按需序列化。
     *
     * <p>与 {@link #generateDetailed} 区别：保留 id/choices 等 OpenAI 标准字段，供 OpenAI SDK 直接接入。
     *
     * <p>Demo 精简点 / 后续迭代替换位置:
     *  - 不解析响应内容，调用方需自行处理 choices/message/content；
     *  - tools/logprobs 等扩展参数按 career-ai 策略静默忽略，后续如需校验应在此处拦截。
     *
     * @throws BizException stream=true / 网络失败 / 鉴权失败 / HTTP 错误 / 返回结构异常时抛出。
     */
    public JsonNode chatCompletionsRaw(List<Map<String, String>> messages,
                                       Double temperature,
                                       Integer maxTokens,
                                       Boolean stream,
                                       String user,
                                       String modelGroup) {
        if (Boolean.TRUE.equals(stream)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "网关暂不支持 stream=true");
        }
        long startMs = System.currentTimeMillis();
        String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
        String requestHash = sha256Short(messages);
        String effectiveModel = (modelGroup == null || modelGroup.isBlank()) ? model : modelGroup;
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("model", effectiveModel);
        payload.put("messages", messages);
        if (temperature != null) {
            payload.put("temperature", temperature);
        }
        if (maxTokens != null) {
            payload.put("max_tokens", maxTokens);
        }
        payload.put("stream", false);
        if (user != null && !user.isBlank()) {
            payload.put("user", user);
        }
        String body;
        try {
            var request = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("X-Request-Id", requestId)
                    .body(payload);
            if (apiKey != null && !apiKey.isBlank()) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }
            body = request.retrieve().body(String.class);
        } catch (BizException exc) {
            writeCallLog(requestId, "gateway_chat_completions", user, null, effectiveModel,
                    System.currentTimeMillis() - startMs, "FAILED", null, requestHash, null);
            throw exc;
        } catch (Exception exc) {
            writeCallLog(requestId, "gateway_chat_completions", user, null, effectiveModel,
                    System.currentTimeMillis() - startMs, "FAILED", null, requestHash, null);
            throw new BizException(ResultCode.INTERNAL_ERROR,
                    "AI 网关调用失败（model=" + effectiveModel + "）：" + exc.getMessage());
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            Integer tokens = null;
            JsonNode usage = root.path("usage");
            if (usage.isObject() && usage.has("total_tokens")) {
                tokens = usage.path("total_tokens").asInt(0);
            }
            writeCallLog(requestId, "gateway_chat_completions", user, null,
                    root.path("model").asText(effectiveModel),
                    System.currentTimeMillis() - startMs, "SUCCESS", tokens, requestHash, null);
            return root;
        } catch (Exception exc) {
            writeCallLog(requestId, "gateway_chat_completions", user, null, effectiveModel,
                    System.currentTimeMillis() - startMs, "FAILED", null, requestHash, null);
            throw new BizException(ResultCode.INTERNAL_ERROR, "AI 网关返回结构异常：" + body);
        }
    }
}
