package com.rickgao.careercore.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生涯咨询请求（POST /api/v1/ai/chat）。
 *
 * <p>Demo 精简点：studentRef 可选（为空走 JWT userId；非空须与 JWT 一致，否则 400），
 * 与 AiPlanGenerateRequest/AiReviewSummarizeRequest 保持一致。
 */
@Data
public class AiChatRequest {

    /** 可选；为空走 JWT userId；非空须与 JWT 一致。 */
    private String studentRef;

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;

    @NotBlank(message = "问题不能为空")
    private String question;

    private AiChatContext context;
}
