package com.rickgao.careercore.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 问卷选项 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOptionVO {

    private String id;
    private String text;
    /** 六维得分 {interest,values,ability,academic,tendency,practice} */
    private Map<String, Double> scores;
}
