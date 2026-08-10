package com.rickgao.careercore.modules.advisor.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辅导员指导意见/建议(advisor_comment)。
 * adviceType: COMMENT / SUGGEST_TASK / SUGGEST_RETEST。
 */
@Data
public class AdvisorComment {

    private String id;
    private String studentId;
    private String advisorId;
    private String content;
    private String adviceType;
    private String suggestedTask;
    private String retestReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
