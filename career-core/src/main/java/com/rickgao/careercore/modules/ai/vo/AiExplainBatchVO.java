package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐解释结果（POST /api/v1/ai/recommendation/explain 200）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExplainBatchVO {

    private String runId;
    private List<AiExplanationItemVO> explanations;
}
