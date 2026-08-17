package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 新增/更新任务模板请求体。对齐 openapi TaskTemplate。 */
@Data
public class TaskTemplateRequest {

    @Size(max = 64, message = "模板 ID 不能超过 64 位")
    private String id;

    @Size(max = 64, message = "方向编码不能超过 64 位")
    private String directionId;

    @Size(max = 100, message = "模板名称不能超过 100 字")
    private String name;

    @Size(max = 2000, message = "目标摘要不能超过 2000 字")
    private String goalSummary;

    private List<SemesterGoal> semesterGoals;
    private List<MonthlyTask> monthlyTasks;
    private String status;

    @Data
    public static class SemesterGoal {
        private String title;
        private String abilityTag;
    }

    @Data
    public static class MonthlyTask {
        private String month;
        private String title;
        private String taskType;
        private Double estimatedHours;
    }
}
