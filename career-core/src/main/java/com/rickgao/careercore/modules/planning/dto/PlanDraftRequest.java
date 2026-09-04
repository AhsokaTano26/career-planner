package com.rickgao.careercore.modules.planning.dto;

import lombok.Data;

/** 生成计划草案请求。 */
@Data
public class PlanDraftRequest {

    private String directionId;
    private Boolean useAi;
    private String requestId;
}

