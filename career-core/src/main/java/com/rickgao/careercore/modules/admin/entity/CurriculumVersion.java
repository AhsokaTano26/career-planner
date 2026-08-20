package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 培养方案版本(curriculum_version)。 */
@Data
public class CurriculumVersion {

    private String id;
    private String name;
    private String major;
    private Integer courseCount;
    private String status;
    private String sourceJobId;
    private LocalDateTime publishedAt;
    private String publishedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
