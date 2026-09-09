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
    /** 状态中文（DRAFT=草稿/CONFIRMED=已确认）。 */
    private String statusName;
    /** AI / TEMPLATE / MANUAL */
    private String source;
    /** 来源中文（AI=智能生成/TEMPLATE=模板生成/MANUAL=手动创建）。 */
    private String sourceName;
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
        /** 能力标签中文名（ability_tag.name，缺失时回退编码）。 */
        private String abilityTagName;
    }

    @Data
    public static class MonthlyTask {
        private String month;
        private String title;
        private String taskType;
        /** 任务类型中文（LEARNING=学习/PRACTICE=实践/CAREER=职业探索/REVIEW=复盘）。 */
        private String taskTypeName;
        private Double estimatedHours;
    }
}
