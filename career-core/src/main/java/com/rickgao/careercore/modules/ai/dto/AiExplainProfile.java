package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

/**
 * 推荐解释-画像六维得分（均可选）。
 */
@Data
public class AiExplainProfile {

    private Double interest;
    private Double values;
    private Double ability;
    private Double academic;
    private Double tendency;
    private Double practice;
}
