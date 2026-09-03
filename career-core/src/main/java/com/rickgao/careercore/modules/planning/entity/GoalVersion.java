package com.rickgao.careercore.modules.planning.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 目标版本历史，对齐 goal_version 表。 */
@Data
public class GoalVersion {

    private String id;
    private String goalId;
    private Integer versionNo;
    private String changeReason;
    private LocalDateTime createdAt;
}
