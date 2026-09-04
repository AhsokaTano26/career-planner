package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

/**
 * 回答反馈请求（POST /api/v1/ai/chat/{messageId}/feedback 与 /chat/feedback）。
 */
@Data
public class AiChatFeedbackRequest {

    /** HELPFUL / NEUTRAL / MISMATCH / NOT_INTERESTED */
    private String feedbackType;

    private String comment;
}
