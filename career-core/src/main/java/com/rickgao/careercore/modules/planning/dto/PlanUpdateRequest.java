package com.rickgao.careercore.modules.planning.dto;

import com.rickgao.careercore.modules.advisor.vo.PlanVO;
import lombok.Data;

/** 编辑计划请求（复用 PlanVO 的 SemesterGoal/MonthlyTask 结构）。 */
@Data
public class PlanUpdateRequest {

    private String goalSummary;
    private java.util.List<PlanVO.SemesterGoal> semesterGoals;
    private java.util.List<PlanVO.MonthlyTask> monthlyTasks;
    private java.util.List<String> notes;
}
