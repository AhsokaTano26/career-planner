package com.career.core.modules.recommendation;

/**
 * 职业方向库实体（career_direction）。
 * personalityTags 为霍兰德（RIASEC）人格类型标签（逗号分隔），用于“人格类型→方向”映射。
 */
public record CareerDirection(
        Long id,
        String directionCode,
        String name,
        String type,
        String status,
        String content,
        String personalityTags) {
}
