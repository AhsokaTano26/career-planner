package com.rickgao.careercore.modules.planning.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 学生目标（PRIMARY / BACKUP），对齐 student_goal 表。 */
@Data
public class StudentGoal {

    private String id;
    private String studentId;
    private String goalType;
    private String directionId;
    private String name;
    private String status;
    private LocalDateTime chosenAt;
    private Integer versionNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

