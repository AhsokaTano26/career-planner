package com.rickgao.careercore.modules.recommendation.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐批次 VO（学生端推荐模块）。
 * 类名加 Rec 前缀避免与 advisor.vo.RecommendationRunVO 的 MyBatis 短别名冲突。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecRunVO {

    private String runId;
    private Integer profileVersion;
    private String ruleVersion;
    private String generatedAt;
    private String status;
    private List<RecResultVO> results;
}
