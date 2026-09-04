package com.rickgao.careercore.modules.assessment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷版本。
 */
@Data
public class QuestionnaireVersion {

    private String id;
    private String questionnaireId;
    private Integer version;
    private String status;
    private Integer questionCount;
    private String changeNote;
    private LocalDateTime publishedAt;
    private String publishedBy;
    private LocalDateTime createdAt;
}

