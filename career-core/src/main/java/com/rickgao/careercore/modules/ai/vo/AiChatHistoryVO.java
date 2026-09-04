package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话历史响应（GET /api/v1/ai/chat/history 200，分页）。
 * 2026-09 Phase 1：原单对象改为分页列表（list + page + total），跨会话合并、按时间倒序。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatHistoryVO {

    private List<Message> list;
    private int page;
    private int size;
    private long total;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String messageId;
        private String sessionId;
        private String role;
        private String content;
        private Boolean needsHumanSupport;
        private String supportReason;
        private LocalDateTime createdAt;
    }
}
