package com.career.core.modules.recommendation;

/**
 * 推荐反馈（线上 Apifox RecommendationFeedback）。
 * feedbackType 取值 HELPFUL / NEUTRAL / MISMATCH / NOT_INTERESTED。
 */
public record RecommendationFeedbackDto(
        String feedbackType,
        String comment) {
}
