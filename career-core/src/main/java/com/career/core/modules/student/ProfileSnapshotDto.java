package com.career.core.modules.student;

import java.util.List;

/**
 * 画像快照响应（线上 Apifox ProfileSnapshot）。
 * dimensions 为六维得分数组；version 为整数版本号；completeness 为整数完整度。
 */
public record ProfileSnapshotDto(
        String id,
        int version,
        String generatedAt,
        String sourceVersion,
        int completeness,
        List<DimensionValueDto> dimensions,
        String summary,
        List<String> strengths,
        List<String> explore,
        ProfileFeedbackDto feedback) {

    /** 六维得分项（线上 DimensionValue） */
    public record DimensionValueDto(String key, String name, double score) {
    }

    /** 画像反馈（线上 ProfileFeedback，feedbackType: MATCH / PARTIAL / MISMATCH） */
    public record ProfileFeedbackDto(String feedbackType, String comment) {
    }
}
