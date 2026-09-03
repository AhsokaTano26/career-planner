package com.rickgao.careercore.modules.recommendation.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推荐结果。
 */
@Data
public class RecommendationResult {

    private String id;
    private String runId;
    private String directionId;
    private BigDecimal score;
    private Integer rank;
    private String confidence;
    private String reasonsJson;
    private String strengthsJson;
    private String gapsJson;
    private String semesterActionsJson;
    private String feedbackJson;
    private LocalDateTime createdAt;
}
