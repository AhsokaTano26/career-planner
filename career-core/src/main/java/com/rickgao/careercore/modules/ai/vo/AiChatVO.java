package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生涯咨询响应（POST /api/v1/ai/chat 200）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatVO {

    /** 消息 ID（用于回答反馈 /api/v1/ai/chat/{messageId}/feedback）。 */
    private String messageId;
    private String answer;
    private java.util.List<String> references;
    private boolean needsHumanSupport;
    private String supportReason;
    private String disclaimer;
}
