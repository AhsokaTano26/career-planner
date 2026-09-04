package com.rickgao.careercore.modules.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.ai.dto.ChatCompletionRequest;
import com.rickgao.careercore.modules.ai.dto.GatewayGenerateRequest;
import com.rickgao.careercore.modules.ai.service.GatewayService;
import com.rickgao.careercore.modules.ai.vo.GatewayGenerateResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 网关对外路由（/api/v1/gateway/*）。
 *
 * <p>当前实现：
 * <ul>
 *   <li>POST /api/v1/gateway/generate —— 高阶文本生成（无 token，由 SecurityConfig 放行）</li>
 *   <li>POST /api/v1/gateway/chat/completions —— OpenAI 兼容聊天补全（透传，无 token）</li>
 * </ul>
 *
 * <p>Demo 精简点 / 后续迭代替换位置:无 token 接口当前不写入 ai_call_log，
 * 若需审计应改为可注入 AuditLogger 并记录 scene/userRef。
 */
@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayController {

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    /** 高阶文本生成：返回模型回复文本 + 元数据（模型/耗时/token）。 */
    @PostMapping("/generate")
    public ApiResponse<GatewayGenerateResponse> generate(@Valid @RequestBody GatewayGenerateRequest req) {
        return ApiResponse.ok(gatewayService.generate(req));
    }

    /**
     * OpenAI 兼容聊天补全（非流式）：透传 career-ai /v1/chat/completions，
     * 返回标准 OpenAI ChatCompletion JSON（id/object/created/model/choices/usage）。
     *
     * <p>不包 ApiResponse：保持 OpenAI SDK 可直接接入。
     */
    @PostMapping("/chat/completions")
    public JsonNode chatCompletions(@Valid @RequestBody ChatCompletionRequest req) {
        return gatewayService.chatCompletions(req);
    }
}
