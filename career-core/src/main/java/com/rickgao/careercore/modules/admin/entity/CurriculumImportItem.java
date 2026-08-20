package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 培养方案待审核课程条目(curriculum_import_item)。 */
@Data
public class CurriculumImportItem {

    private String id;
    private String jobId;
    private String courseCode;
    private String courseName;
    private String semester;
    private BigDecimal credits;
    private BigDecimal hours;
    private String category;
    private String module;
    private String prerequisitesJson;
    private String abilityTagsJson;
    private BigDecimal confidence;
    private String pageRef;
    private String status;
    private String mergedInto;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
