package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 阶段总结结果（POST /api/v1/ai/review/summarize 200）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewSummaryVO {

    private String summary;
    private List<String> suggestions;
}
