package com.rickgao.careercore.modules.assessment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷题目。
 */
@Data
public class Question {

    private String id;
    private String questionnaireVersionId;
    private String text;
    private String type;
    private String dim;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
