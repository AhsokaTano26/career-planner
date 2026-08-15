package com.career.core.modules.planning;

import java.util.List;

/**
 * 学期计划（线上 Apifox Plan）。
 * id 为字符串计划ID（如 PLAN-1002）；version 为计划版本号；source 取值 AI/TEMPLATE/MANUAL；
 * status 取值 DRAFT/CONFIRMED。
 */
public record PlanDto(
        String id,
        String version,
        String status,
        String source,
        String goalSummary,
        List<SemesterGoalDto> semesterGoals,
        List<MonthlyTaskDto> monthlyTasks,
        List<String> notes,
        String confirmedAt,
        String updatedAt) {

    /** 学期目标（线上 SemesterGoal） */
    public record SemesterGoalDto(String title, String abilityTag) {
    }

    /** 月度任务（线上 MonthlyTask，taskType: LEARNING/PRACTICE/CAREER/REVIEW） */
    public record MonthlyTaskDto(String month, String title, String taskType, double estimatedHours) {
    }
}
