package com.career.core.modules.planning;

/** 学期计划（线上 Apifox Plan 简化版）。 */
public record PlanDto(
        Long id,
        Long goalId,
        String semester,
        String source,
        String status) {
}
