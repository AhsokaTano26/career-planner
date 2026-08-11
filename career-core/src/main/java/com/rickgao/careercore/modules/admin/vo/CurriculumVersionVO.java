package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 培养方案版本。对齐 openapi CurriculumVersion。 */
@Data
public class CurriculumVersionVO {

    private String id;
    private String name;
    private String major;
    private Integer courseCount;
    private String status;
    private LocalDateTime publishedAt;
    private String publishedBy;
}
