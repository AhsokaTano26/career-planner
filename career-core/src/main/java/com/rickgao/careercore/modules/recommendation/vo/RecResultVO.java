package com.rickgao.careercore.modules.recommendation.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐结果 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecResultVO {

    private String resultId;
    private String directionId;
    private Integer rank;
    private Double score;
    private String confidence;
    private List<String> reasons;
    private List<String> strengths;
    private List<String> gaps;
    private List<String> semesterActions;
    private RecFeedbackVO feedback;
}
