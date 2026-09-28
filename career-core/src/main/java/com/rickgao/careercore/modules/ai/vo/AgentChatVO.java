package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体对话响应（POST /api/v1/ai/agent/invoke 200）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatVO {

    private String answer;
    private List<String> references;
    private boolean needsHumanSupport;
    private String supportReason;
    private String disclaimer;
}
