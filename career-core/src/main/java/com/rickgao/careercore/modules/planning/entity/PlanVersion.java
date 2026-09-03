package com.rickgao.careercore.modules.planning.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 计划版本历史，对齐 plan_version 表。 */
@Data
public class PlanVersion {

    private String id;
    private String planId;
    private Integer versionNo;
    private String contentJson;
    private LocalDateTime createdAt;
}
