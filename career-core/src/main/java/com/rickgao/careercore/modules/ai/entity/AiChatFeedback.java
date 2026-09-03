package com.rickgao.careercore.modules.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话反馈（ai_chat_feedback）。
 * 一条 message_group（一次问答对）唯一对应一条反馈（UNIQUE KEY uk_message_group）。
 */
@Data
public class AiChatFeedback {

    private String id;
    private String messageGroup;
    private String userId;
    private String feedbackType;
    private String comment;
    private LocalDateTime createdAt;
}
