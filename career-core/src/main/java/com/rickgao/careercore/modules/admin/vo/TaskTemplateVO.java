package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.util.List;

/** 任务模板。对齐 openapi TaskTemplate。 */
@Data
public class TaskTemplateVO {

    private String id;
    private String directionId;
    private String name;
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
