package com.rickgao.careercore.modules.planning.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 学期计划，对齐 semester_plan 表。 */
@Data
public class SemesterPlan {

    private String id;
    private String studentId;
    private Integer versionNo;
    private String status;
    private String source;
    private String goalSummary;
    private String semesterGoalsJson;
    private String monthlyTasksJson;
    private String notesJson;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
