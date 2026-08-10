package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 推荐批次。对齐 openapi RecommendationRun。
 */
@Data
public class RecommendationRunVO {

    private String runId;
    private Integer profileVersion;
    private String ruleVersion;
    private LocalDateTime generatedAt;
    /** RUNNING / SUCCESS / DEGRADED / FAILED */
    private String status;
    private List<RecommendationResult> results;

    @Data
    public static class RecommendationResult {
        private String directionId;
        private Integer rank;
        private Double score;
        private String confidence;
        private List<String> reasons;
        private List<String> strengths;
        private List<String> gaps;
        private List<String> semesterActions;
        private RecommendationFeedback feedback;
    }

    @Data
    public static class RecommendationFeedback {
        private String feedbackType;
        private String comment;
    }
}
