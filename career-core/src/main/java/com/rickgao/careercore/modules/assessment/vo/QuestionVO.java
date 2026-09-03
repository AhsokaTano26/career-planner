package com.rickgao.careercore.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问卷题目 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionVO {

    private String id;
    private String text;
    private String type;
    private String dim;
    private List<String> labels;
    private List<QuestionOptionVO> options;
}
