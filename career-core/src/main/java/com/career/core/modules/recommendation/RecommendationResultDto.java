package com.career.core.modules.recommendation;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 推荐结果项（线上 Apifox RecommendationResult）。
 * directionId 为方向编码（career_direction.direction_code，字符串）；
 * score 为百分制匹配度（0-100）；confidence 为 HIGH/MEDIUM/LOW 枚举。
 * feedback 为非必填对象：无反馈时省略该字段（Apifox 契约测试不允许对象字段为 null），
 * 故类级 @JsonInclude(NON_NULL) 只对 null 字段生效，不影响空列表等。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationResultDto(
        String directionId,
        int rank,
        double score,
        String confidence,
        List<String> reasons,
        List<String> strengths,
        List<String> gaps,
        List<String> semesterActions,
        RecommendationFeedbackDto feedback) {
}
