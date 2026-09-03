package com.rickgao.careercore.modules.assessment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问卷选项。
 */
@Data
public class QuestionOption {

    private String id;
    private String questionId;
    private String text;
    /** 六维得分 JSON {interest,values,ability,academic,tendency,practice} */
    private String scoresJson;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
