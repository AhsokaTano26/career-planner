package com.rickgao.careercore.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学期目标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSemesterGoalVO {

    private String title;
    private String abilityTag;
}
