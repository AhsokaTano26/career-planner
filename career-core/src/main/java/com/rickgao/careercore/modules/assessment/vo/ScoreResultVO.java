package com.rickgao.careercore.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 计分结果 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResultVO {

    private String sessionId;
    private String status;
    private List<DimensionScoreVO> dimensionScores;
}
