package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

/**
 * 复盘总结请求（POST /api/v1/ai/review/summarize）。
 */
@Data
public class AiReviewSummarizeRequest {

    private String studentRef;

    private String cycle;

    private AiReviewContent reviewContent;

    private String taskSummary;
}
