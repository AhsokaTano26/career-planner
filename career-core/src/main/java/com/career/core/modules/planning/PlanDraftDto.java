package com.career.core.modules.planning;

import java.util.List;

/**
 * 计划草案响应（线上 Apifox PlanDraft）。
 */
public record PlanDraftDto(
        String goalSummary,
        List<SemesterGoalDto> semesterGoals,
        List<MonthlyTaskDto> monthlyTasks,
        List<String> notes) {

    /** 学期目标（线上 SemesterGoal） */
    public record SemesterGoalDto(String title, String abilityTag) {
    }

    /** 月度任务（线上 MonthlyTask，taskType: LEARNING/PRACTICE/CAREER/REVIEW） */
    public record MonthlyTaskDto(String month, String title, String taskType, double estimatedHours) {
    }
}
