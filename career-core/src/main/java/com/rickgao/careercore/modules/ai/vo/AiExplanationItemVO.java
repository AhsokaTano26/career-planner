package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐解释-单条。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExplanationItemVO {

    private String directionId;
    private String summary;
    private String confidenceText;
    private String disclaimer;
}
