package com.rickgao.careercore.modules.recommendation.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐批次。
 */
@Data
public class RecommendationRun {

    private String id;
    private String studentId;
    private String profileSnapshotId;
    private String ruleVersion;
    private String status;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
}

