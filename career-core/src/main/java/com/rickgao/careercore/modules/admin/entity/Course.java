package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 正式课程(course,发布时由 APPROVED 条目生成)。 */
@Data
public class Course {

    private String id;
    private String versionId;
    private String courseCode;
    private String courseName;
    private String semester;
    private BigDecimal credits;
    private BigDecimal hours;
    private String category;
    private String module;
    private String prerequisitesJson;
    private String sourceItemId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
