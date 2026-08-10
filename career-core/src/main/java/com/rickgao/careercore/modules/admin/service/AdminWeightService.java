package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.modules.admin.dto.WeightConfigRequest;
import com.rickgao.careercore.modules.admin.vo.WeightConfigVO;

/** 管理端-推荐权重应用服务。 */
public interface AdminWeightService {

    WeightConfigVO getWeights();

    WeightConfigVO createWeights(String operatorId, String endpoint, String idempotencyKey,
                                 WeightConfigRequest request);
}
