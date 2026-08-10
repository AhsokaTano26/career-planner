package com.rickgao.careercore.modules.student.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生经历实体(student_experience)。
 */
@Data
public class StudentExperience {

    private String id;
    /** 所属学生用户 ID */
    private String studentId;
    /** 类别:竞赛/项目/学生工作/志愿服务 */
    private String type;
    private String title;
    /** YYYY-MM */
    private String startDate;
    /** YYYY-MM(选填) */
    private String endDate;
    private String description;
    private String attachmentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
}
