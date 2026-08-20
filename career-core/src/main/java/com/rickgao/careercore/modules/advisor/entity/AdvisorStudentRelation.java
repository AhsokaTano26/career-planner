package com.rickgao.careercore.modules.advisor.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辅导员-学生管理关系(advisor_student_relation)。
 * 辅导员数据范围校验的唯一依据。
 */
@Data
public class AdvisorStudentRelation {

    private String id;
    private String advisorId;
    private String studentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
