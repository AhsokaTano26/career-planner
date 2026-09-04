package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 计划生成请求（POST /api/v1/ai/plan/generate）。
 */
@Data
public class AiPlanGenerateRequest {

    private String studentRef;

    private String directionId;

    private String semester;

    private String goalSummary;

    /** 参考模板（可选，PlanDraft 结构）。 */
    private AiPlanDraft template;
}
