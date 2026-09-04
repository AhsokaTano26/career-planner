package com.rickgao.careercore.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问卷详情 VO（含题目与选项）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireDetailVO {

    private QuestionnaireVO questionnaire;
    private List<QuestionVO> questions;
}

