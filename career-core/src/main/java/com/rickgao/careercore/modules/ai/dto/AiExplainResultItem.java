package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

/**
 * 推荐解释-候选方向条目。
 */
@Data
public class AiExplainResultItem {

    private String directionId;
    private Double score;
    private Integer rank;
}
