package com.rickgao.careercore.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问卷版本 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireVersionVO {

    private String id;
    private Integer version;
    private String status;
    private String publishedAt;
    private String publishedBy;
    private Integer questionCount;
    private String changeNote;
}

