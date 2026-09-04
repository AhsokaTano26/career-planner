package com.rickgao.careercore.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 六维得分项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionScoreVO {

    private String dimensionCode;
    private String dimensionName;
    private Double score;
}

