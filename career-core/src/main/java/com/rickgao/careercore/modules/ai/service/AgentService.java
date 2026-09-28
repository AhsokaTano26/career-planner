package com.rickgao.careercore.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.ai.dto.AgentChatRequest;
import com.rickgao.careercore.modules.ai.vo.AgentChatVO;
import com.rickgao.careercore.security.LoginUser;
import com.rickgao.careercore.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体服务：career-core 代理转发到 career-ai 的 LangGraph 智能体
 * （{@code POST /api/v1/ai/agent/invoke}，工具调用 + RAG + 多轮记忆）。
 *
 * <p>配置（环境变量注入，不硬编码）：AI_GATEWAY_BASE_URL / AI_GATEWAY_API_KEY（与 LlmGateway 同源）、
 * AI_AGENT_TIMEOUT（可选，秒，默认 120——智能体多步工具调用耗时高于普通网关调用）。
 *
 * <p>数据范围：STAFF（ADMIN/ADVISOR）可指定任意 studentRef；STUDENT 只能查询本人，
 * 否则抛 FORBIDDEN。工具调用所需的内部令牌由 career-ai 侧持有，此处仅约束数据范围。
 */
@Service
public class AgentService {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:8000";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_ADVISOR = "ADVISOR";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AgentService(@Value("${ai.gateway-api-key:}") String apiKey,
                        @Value("${ai.gateway-base-url:}") String baseUrl,
                        @Value("${ai.agent-timeout:120}") int timeout,
                        ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        String effectiveBaseUrl = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(effectiveBaseUrl.replaceAll("/+$", ""))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(RestClientFactory.factory(timeout))
                .build();
    }

    public AgentChatVO invoke(AgentChatRequest req) {
        String studentRef = resolveStudentRef(req.getStudentRef());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentRef", studentRef);
        payload.put("sessionId", req.getSessionId());
        payload.put("question", req.getQuestion());
        if (req.getContext() != null) {
            payload.put("context", req.getContext());
        }
        String body;
        try {
            var request = restClient.post()
                    .uri("/api/v1/ai/agent/invoke")
                    .body(payload);
            if (apiKey != null && !apiKey.isBlank()) {
                request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }
            body = request.retrieve().body(String.class);
        } catch (Exception exc) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "智能体服务调用失败：" + exc.getMessage());
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return AgentChatVO.builder()
                    .answer(root.path("answer").asText(""))
                    .references(toList(root.path("references")))
                    .needsHumanSupport(root.path("needsHumanSupport").asBoolean(false))
                    .supportReason(root.path("supportReason").asText(""))
                    .disclaimer(root.path("disclaimer").asText(""))
                    .build();
        } catch (Exception exc) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "智能体服务返回结构异常：" + body);
        }
    }

    /** STAFF（ADMIN/ADVISOR）可跨学生；STUDENT 只能查询本人（studentRef 需等于其内部 ID 或登录账号/学号）。 */
    private String resolveStudentRef(String studentRef) {
        LoginUser user = SecurityUtils.currentUser();
        String role = user.getRole();
        boolean staff = ROLE_ADMIN.equals(role) || ROLE_ADVISOR.equals(role);
        boolean self = studentRef.equals(user.getId()) || studentRef.equals(user.getUsername());
        if (!staff && !self) {
            throw new BizException(ResultCode.FORBIDDEN, "学生只能查询本人数据");
        }
        return studentRef;
    }

    private List<String> toList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> list.add(item.asText("")));
        }
        return list;
    }
}
