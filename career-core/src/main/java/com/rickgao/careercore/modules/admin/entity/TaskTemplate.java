package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 任务模板(task_template)。 */
@Data
public class TaskTemplate {

    private String id;
    private String directionId;
    private String name;
    private String goalSummary;
    private String semesterGoalsJson;
    private String monthlyTasksJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
