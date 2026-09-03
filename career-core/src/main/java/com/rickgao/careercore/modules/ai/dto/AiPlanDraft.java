package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 计划草案结构（goalSummary / semesterGoals / monthlyTasks / notes）。
 */
@Data
public class AiPlanDraft {

    private String goalSummary;
    private List<AiSemesterGoal> semesterGoals;
    private List<AiMonthlyTask> monthlyTasks;
    private List<String> notes;
}
