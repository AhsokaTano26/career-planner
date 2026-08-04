package com.career.core.modules.recommendation;

/**
 * 接口2 返回的方向对象。
 * 线上结构：directionId / score / rank / confidence；增强字段：name / type / reason（线上未定义）。
 * confidence 为归一化概率表达（0-1，softmax 归一化，本次推荐结果集合内求和约等于 1），
 * 表示该方向在本次推荐结果中的相对把握度（Demo 精简点：替代原先 HIGH/MEDIUM/LOW 枚举）。
 */
public record RecommendationDto(
        Long directionId,
        String name,
        String type,
        double score,
        int rank,
        double confidence,
        String reason) {
}
