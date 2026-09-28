package com.rickgao.careercore.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能体对话请求（POST /api/v1/ai/agent/invoke）。
 *
 * <p>studentRef 权限：STAFF（ADMIN/ADVISOR）可指定任意学生；STUDENT 必须与 JWT 用户一致，
 * 由 AgentService 校验（工具调用所需的内部令牌由 career-ai 侧持有，此处仅做数据范围约束）。
 */
@Data
public class AgentChatRequest {

    @NotBlank(message = "studentRef 不能为空")
    private String studentRef;

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @NotBlank(message = "问题不能为空")
    private String question;

    /** 可选上下文（directionId / goalSummary），与 /ai/chat 复用同一结构。 */
    private AiChatContext context;
}
