package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 计划任务查询行 */
@Data
public class TaskRow {

    private String id;
    private String planId;
    private String studentId;
    private String month;
    private String title;
    private String taskType;
    private Double estHours;
    private String status;
    private LocalDate deadline;
    private String abilityTagsJson;
    private String note;
    private LocalDateTime createdAt;
}
