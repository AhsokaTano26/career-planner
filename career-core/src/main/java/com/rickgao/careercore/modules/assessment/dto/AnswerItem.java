package com.rickgao.careercore.modules.assessment.dto;

import lombok.Data;

/**
 * 答案项。
 */
@Data
public class AnswerItem {

    private String questionId;
    /** CHOICE 题型：选中选项序号（0 起） */
    private Integer optionIndex;
    /** RATING 题型：1-5 */
    private Integer ratingValue;
}
