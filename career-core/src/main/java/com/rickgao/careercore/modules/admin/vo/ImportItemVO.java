package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.util.List;

/** 待审核课程条目。对齐 openapi ImportItem。 */
@Data
public class ImportItemVO {

    private String id;
    private String jobId;
    private String courseCode;
    private String courseName;
    private String semester;
    private Double credits;
    private Double hours;
    private String category;
    private String module;
    private List<String> prerequisites;
    private List<String> abilityTags;
    private Double confidence;
    private String pageRef;
    private String status;
}
