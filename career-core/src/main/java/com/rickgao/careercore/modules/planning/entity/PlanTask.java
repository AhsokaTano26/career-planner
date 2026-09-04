package com.rickgao.careercore.modules.planning.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 计划任务，对齐 plan_task 表。 */
@Data
public class PlanTask {

    private String id;
    private String planId;
    private String studentId;
    private String month;
    private String title;
    private String taskType;
    private BigDecimal estHours;
    private String status;
    private LocalDate deadline;
    private String abilityTagsJson;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

