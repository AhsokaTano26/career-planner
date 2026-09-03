package com.rickgao.careercore.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 问卷列表项 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionnaireVO {

    private String id;
    private String type;
    private String typeName;
    private String icon;
    private Integer version;
    private String status;
    private Integer questionCount;
    private Integer minutes;
    private String tip;
    private String publishedAt;
}
