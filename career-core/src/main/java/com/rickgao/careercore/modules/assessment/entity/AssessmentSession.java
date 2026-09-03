package com.rickgao.careercore.modules.assessment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测评会话。
 */
@Data
public class AssessmentSession {

    private String id;
    private String studentId;
    private String questionnaireVersionId;
    private String status;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime finishedAt;
    /** 六维得分 JSON [{dimensionCode,dimensionName,score}] */
    private String scoreJson;
    private String requestId;
    private LocalDateTime createdAt;
}
