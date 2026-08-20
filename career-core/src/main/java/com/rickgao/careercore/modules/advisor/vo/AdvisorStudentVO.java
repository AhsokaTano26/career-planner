package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 所带学生列表项。对齐 openapi AdvisorStudent。
 */
@Data
public class AdvisorStudentVO {

    private String id;
    private String name;
    private String className;
    private Integer completeness;
    private Boolean assessed;
    /** graduate / employment / overseas */
    private String path;
    private String direction;
    private String primaryGoal;
    /** 计划完成率(%):已完成任务 / 总任务 */
    private Integer planRate;
    private LocalDate lastReview;
    private Boolean askGuidance;
    /** good / todo / late / review */
    private String status;
}
