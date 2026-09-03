package com.rickgao.careercore.modules.planning.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 阶段复盘，对齐 stage_review 表。 */
@Data
public class StageReview {

    private String id;
    private String studentId;
    private String cycle;
    private String status;
    private String contentJson;
    private String aiSummary;
    private String aiSuggestJson;
    private Boolean advisorRequested;
    private String advisorReply;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
