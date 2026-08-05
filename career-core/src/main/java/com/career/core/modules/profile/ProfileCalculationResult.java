package com.career.core.modules.profile;

import java.util.List;
import java.util.Map;

/** 六维画像计算结果，不包含任何大模型生成分数。 */
public record ProfileCalculationResult(
        Map<String, ProfileDimensionResult> dimensions,
        double completeness,
        List<String> strengths,
        List<String> explorationQuestions,
        String summary) {
}
