package com.rickgao.careercore.modules.assessment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷。
 */
@Data
public class Questionnaire {

    private String id;
    private String type;
    private String name;
    private String typeName;
    private String icon;
    private String status;
    private Integer version;
    private Integer minutes;
    private String tip;
    private LocalDateTime publishedAt;
    private String publishedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

