package com.career.core.modules.profile;

import com.career.core.common.BadRequestException;

import java.util.Locale;

/** 学生对画像描述的符合程度反馈，与 Apifox 枚举保持一致。 */
public enum ProfileFeedbackType {
    MATCH,
    PARTIAL,
    MISMATCH;

    public static ProfileFeedbackType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("feedbackType 不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("feedbackType 仅支持 MATCH、PARTIAL、MISMATCH");
        }
    }
}
