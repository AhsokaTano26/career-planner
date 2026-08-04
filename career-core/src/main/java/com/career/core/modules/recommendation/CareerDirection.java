package com.career.core.modules.recommendation;

/** 职业方向库实体（career_direction） */
public record CareerDirection(
        Long id,
        String directionCode,
        String name,
        String type,
        String status,
        String content) {
}
