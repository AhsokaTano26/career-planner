package com.rickgao.careercore.modules.admin.query;

import lombok.Data;

import java.time.LocalDateTime;

/** 学生数据导出行。 */
@Data
public class StudentExportRow {

    private String studentNo;
    private String name;
    private String className;
    private String grade;
    private String majorCategory;
    private Integer completeness;
    private String path;
    private Boolean assessed;
    private String primaryGoal;
    private LocalDateTime lastReview;
}
