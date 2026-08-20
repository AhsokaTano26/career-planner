package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

import java.time.LocalDateTime;

/** 学期计划查询行 */
@Data
public class PlanRow {

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
    private LocalDateTime updatedAt;
}
