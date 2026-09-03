package com.rickgao.careercore.modules.assessment.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测评会话 VO（学生端）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSessionVO {

    private String id;
    private String questionnaireId;
    private String questionnaireName;
    private Integer questionnaireVersion;
    private String status;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private String startedAt;
    private String updatedAt;
    private String finishedAt;
}
