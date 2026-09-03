package com.rickgao.careercore.modules.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话消息记录（ai_chat_message）。
 * 一次问答写入 2 条：role=user（学生提问）+ role=assistant（AI 回答），
 * 共享同一 message_group（即原 messageId，前端反馈按 message_group 关联）。
 */
@Data
public class AiChatMessage {

    private String id;
    private String sessionId;
    private String userId;
    private String role;
    private String content;
    private Boolean needsHumanSupport;
    private String supportReason;
    private String messageGroup;
    private LocalDateTime createdAt;
}
