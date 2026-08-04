package com.career.core.modules.recommendation;

/**
 * 接口2 返回的方向对象。
 * 线上结构：directionId / score / rank / confidence；增强字段：name / type / reason（线上未定义）。
 */
public record RecommendationDto(
        Long directionId,
        String name,
        String type,
        double score,
        int rank,
        String confidence,
        String reason) {
}
