package com.rickgao.careercore.modules.portrait.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 画像反馈请求。
 */
@Data
public class ProfileFeedbackRequest {

    @NotBlank(message = "feedbackType 不能为空")
    private String feedbackType;

    private String comment;
}

