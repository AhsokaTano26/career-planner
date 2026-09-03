package com.rickgao.careercore.modules.admin.dto;

import lombok.Data;

/** 管理端问卷状态更新请求。 */
@Data
public class QuestionnaireStatusUpdate {

    /** PUBLISHED / DISABLED / DRAFT */
    private String status;
}
