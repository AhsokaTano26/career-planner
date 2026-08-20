package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指导意见/建议返回。对齐 openapi GuidanceComment。
 */
@Data
public class GuidanceCommentVO {

    private String id;
    private String studentId;
    private String content;
    private String adviceType;
    private String suggestedTask;
    private String retestReason;
    private LocalDateTime createdAt;
}
