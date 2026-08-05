package com.career.core.modules.profile;

/** 画像反馈请求体。 */
public record ProfileFeedbackRequest(String feedbackType, String comment) {
}
