package com.rickgao.careercore.modules.ai.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.ai.dto.AgentChatRequest;
import com.rickgao.careercore.modules.ai.service.AgentService;
import com.rickgao.careercore.modules.ai.vo.AgentChatVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能体路由（/api/v1/ai/agent/*）：代理转发 career-ai 的 LangGraph 智能体
 * （工具调用 + RAG + 多轮记忆），供前端 AI 调试台调用。需 JWT。
 *
 * <p>Demo 精简点：当前仅暴露 /invoke；career-ai 侧的反馈/历史端点暂未接入。
 */
@RestController
@RequestMapping("/api/v1/ai/agent")
@Tag(name = "AI 智能体")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /** 智能体对话（多轮：同一 sessionId 复用线程记忆）。 */
    @PostMapping("/invoke")
    public ApiResponse<AgentChatVO> invoke(@Valid @RequestBody AgentChatRequest req) {
        return ApiResponse.ok(agentService.invoke(req));
    }
}
