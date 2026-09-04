package com.rickgao.careercore.modules.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 推荐反馈请求。
 */
@Data
public class RecommendationFeedbackRequest {

    @NotBlank(message = "feedbackType 不能为空")
    private String feedbackType;

    private String comment;
}

