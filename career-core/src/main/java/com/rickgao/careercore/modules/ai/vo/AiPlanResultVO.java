package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 计划生成结果（POST /api/v1/ai/plan/generate 200）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPlanResultVO {

    private String goalSummary;
    private List<AiSemesterGoalVO> semesterGoals;
    private List<AiMonthlyTaskVO> monthlyTasks;
    private List<String> notes;
}
