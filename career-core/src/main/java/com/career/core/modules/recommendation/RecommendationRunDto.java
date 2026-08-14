package com.career.core.modules.recommendation;

import java.util.List;

/**
 * 推荐批次（线上 Apifox RecommendationRun）。
 * runId 为批次 ID（字符串）；status 取值 RUNNING / SUCCESS / DEGRADED / FAILED。
 */
public record RecommendationRunDto(
        String runId,
        int profileVersion,
        String ruleVersion,
        String generatedAt,
        String status,
        List<RecommendationResultDto> results) {
}
