package com.rickgao.careercore.modules.assessment.dto;

import lombok.Data;

import java.util.List;

/**
 * 保存答案请求。
 */
@Data
public class SaveAnswersRequest {

    private String requestId;

    private List<AnswerItem> answers;

    private Boolean finished;
}

