package com.rickgao.careercore.modules.recommendation.dto;

import lombok.Data;

/**
 * 创建推荐批次请求。
 */
@Data
public class CreateRecommendationRequest {

    /** 发展路径过滤（graduate/employment/overseas），可选 */
    private String pathFilter;

    /** 幂等请求 ID */
    private String requestId;
}

