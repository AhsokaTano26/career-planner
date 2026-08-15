package com.career.core.modules.student;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 画像快照响应（线上 Apifox ProfileSnapshot）。
 * dimensions 为六维得分数组；version 为整数版本号；completeness 为整数完整度。
 * feedback 为非必填对象：无反馈时省略该字段（Apifox 契约测试不允许对象字段为 null），
 * 故类级 @JsonInclude(NON_NULL) 只对 null 字段生效。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
