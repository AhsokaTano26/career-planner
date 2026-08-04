package com.career.core.integration.ai;

import com.career.core.modules.recommendation.RecommendationEngine;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * career-ai（FastAPI 智能服务）推荐解释客户端。
 * 职责：将结构化推荐结果（方向 + 评分 + 维度匹配 + 差距 + 霍兰德人格）POST 到
 * career-ai 的 {@code POST /v1/recommendation/explain}，由其调用大模型生成自然语言解释。
 * <p>
 * Demo 精简点 / 后续迭代替换位置：
 *   - 失败/超时/未启用时抛出异常，由调用方（RecommendationService）回退规则模板；
 *   - 请求不携带学生身份（student_id 留空），符合“career-ai 不持有学生身份信息”的边界约定；
 *   - 后续可扩展 HMAC 签名、request-id 透传、ai_call_log 记录（见《具体实现细节_MVP_V1.0.md》）。
 */
@Component
public class AiExplainClient {

    private final RestClient restClient;
    private final boolean enabled;
    @SuppressWarnings("unused")
    private final String model;

    public AiExplainClient(
            @Value("${career.ai.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${career.ai.enabled:true}") boolean enabled,
            @Value("${career.ai.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${career.ai.timeout-ms:15000}") int readTimeoutMs,
            @Value("${career.ai.model:deepseek-v4-pro}") String model) {
        this.enabled = enabled;
        this.model = model;
        // 连接超时保持短（AI 服务不可用时快速回退）；读取超时放宽（真实 LLM 生成解释需数秒）
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 调用 career-ai 生成推荐解释。
     *
     * @return 自然语言推荐理由
     * @throws IllegalStateException 未启用、调用失败或返回为空时抛出（由调用方回退模板）
     */
    public String explain(RecommendationEngine.ScoredDirection scored, String studentPersonality) {
        if (!enabled) {
            throw new IllegalStateException("career-ai 未启用（career.ai.enabled=false），回退模板理由");
        }
        ExplainRequest request = buildRequest(scored, studentPersonality);
        ExplainResponse resp = restClient.post()
                .uri("/v1/recommendation/explain")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ExplainResponse.class);
        if (resp == null || resp.reason() == null || resp.reason().isBlank()) {
            throw new IllegalStateException("career-ai 返回的解释为空，回退模板理由");
        }
        return resp.reason();
    }

    private ExplainRequest buildRequest(RecommendationEngine.ScoredDirection scored, String studentPersonality) {
        ExplainRequest.Direction direction = new ExplainRequest.Direction(
                scored.direction().id(),
                scored.direction().name(),
                scored.direction().type(),
                scored.score(),
                parseTags(scored.direction().personalityTags()),
                scored.matches(),
                scored.gaps());
        return new ExplainRequest(null, parseTags(studentPersonality), direction);
    }

    /** 解析霍兰德标签串（支持 "R,I,C" 与连续编码 "IRC"），统一拆分为单字母 RIASEC 编码 */
    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split("[,/]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .flatMap(s -> Arrays.stream(s.split("")))
                .distinct()
                .toList();
    }

    /** 请求体（与 career-ai 的 pydantic ExplainRequest 字段对齐，snake_case） */
    public record ExplainRequest(
            @JsonProperty("student_id") Long studentId,
            List<String> personality,
            Direction direction) {

        public record Direction(
                @JsonProperty("direction_id") Long directionId,
                String name,
                String type,
                double score,
                @JsonProperty("personality_tags") List<String> personalityTags,
                Map<String, Double> matches,
                Map<String, Double> gaps) {
        }
    }

    /** 响应体（career-ai 返回：自然语言理由 + 所用模型名） */
    public record ExplainResponse(String reason, String model) {
    }
}
