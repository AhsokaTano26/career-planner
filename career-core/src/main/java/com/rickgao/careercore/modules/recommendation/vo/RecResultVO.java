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
    /** 方向中文名（前端直接展示，不再裸露 directionId 编码）。 */
    private String directionName;
    private Integer rank;
    private Double score;
    private String confidence;
    /** 置信度中文（HIGH=匹配度高/MEDIUM=匹配度中/LOW=匹配度一般）。 */
    private String confidenceName;
    private List<String> reasons;
    private List<String> strengths;
    private List<String> gaps;
    private List<String> semesterActions;
    private RecFeedbackVO feedback;
}

