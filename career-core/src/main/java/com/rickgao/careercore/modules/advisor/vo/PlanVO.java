package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学期计划。对齐 openapi Plan。
 */
@Data
public class PlanVO {

    private String id;
    private String version;
    /** DRAFT / CONFIRMED */
    private String status;
    /** AI / TEMPLATE / MANUAL */
    private String source;
    private String goalSummary;
    private List<SemesterGoal> semesterGoals;
    private List<MonthlyTask> monthlyTasks;
    private List<String> notes;
    private LocalDateTime confirmedAt;
    private LocalDateTime updatedAt;

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
