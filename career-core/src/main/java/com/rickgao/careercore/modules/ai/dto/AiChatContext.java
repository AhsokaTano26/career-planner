package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

/**
 * 生涯咨询上下文（可选：directionId / goalSummary）。
 */
@Data
public class AiChatContext {

    private String directionId;
    private String goalSummary;
}
