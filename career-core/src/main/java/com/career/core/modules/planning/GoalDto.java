package com.career.core.modules.planning;

/** 学生目标（线上 Apifox Goal 简化版）。 */
public record GoalDto(
        Long id,
        Long directionId,
        String title,
        String goalType,
        String status) {
}
