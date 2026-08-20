package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 推荐权重版本(recommendation_weight)。 */
@Data
public class RecommendationWeight {

    private String id;
    private String version;
    private String weightsJson;
    private BigDecimal minConfidence;
    private Integer topN;
    private String status;
    private LocalDateTime publishedAt;
    private String publishedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
