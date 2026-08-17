package com.rickgao.careercore.modules.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.dto.WeightConfigRequest;
import com.rickgao.careercore.modules.admin.entity.RecommendationWeight;
import com.rickgao.careercore.modules.admin.mapper.AdminWeightMapper;
import com.rickgao.careercore.modules.admin.service.AdminWeightService;
import com.rickgao.careercore.modules.admin.vo.WeightConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/** 管理端-推荐权重服务实现(POST 创建 DRAFT,发布接口由后续提供)。 */
@Service
public class AdminWeightServiceImpl implements AdminWeightService {

    private static final double SUM_TOLERANCE = 0.001;

    private final AdminWeightMapper weightMapper;
    private final IdGenerator idGenerator;
    private final IdempotencyService idempotencyService;

    public AdminWeightServiceImpl(AdminWeightMapper weightMapper,
                                  IdGenerator idGenerator,
                                  IdempotencyService idempotencyService) {
        this.weightMapper = weightMapper;
        this.idGenerator = idGenerator;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public WeightConfigVO getWeights() {
        RecommendationWeight weight = weightMapper.findLatestPublished();
        return weight == null ? null : toVO(weight);
    }

    @Override
    @Transactional
    public WeightConfigVO createWeights(String operatorId, String endpoint, String idempotencyKey,
                                        WeightConfigRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, WeightConfigVO.class,
                () -> ApiResponse.ok(doCreateWeights(request))).getData();
    }

    private WeightConfigVO doCreateWeights(WeightConfigRequest request) {
        validateWeights(request);
        if (weightMapper.findByVersion(request.getVersion().trim()) != null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "权重版本号已存在");
        }
        RecommendationWeight weight = new RecommendationWeight();
        weight.setId(idGenerator.weightId());
        weight.setVersion(request.getVersion().trim());
        weight.setWeightsJson(JsonUtil.toJson(request.getWeights()));
        weight.setMinConfidence(request.getMinConfidence() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(request.getMinConfidence()));
        weight.setTopN(request.getTopN() == null ? 5 : request.getTopN());
        weight.setStatus("DRAFT");
        weightMapper.insert(weight);
        return toVO(weight);
    }

    private void validateWeights(WeightConfigRequest request) {
        if (request.getWeights() == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "weights 六维权重必填");
        }
        WeightConfigRequest.Weights w = request.getWeights();
        double[] values = {
                require(w.getInterest(), "weights.interest"),
                require(w.getValues(), "weights.values"),
                require(w.getAbility(), "weights.ability"),
                require(w.getAcademic(), "weights.academic"),
                require(w.getTendency(), "weights.tendency"),
                require(w.getPractice(), "weights.practice")
        };
        double sum = 0;
        for (double v : values) {
            if (v < 0 || v > 1) {
                throw new BizException(ResultCode.VALIDATION_ERROR, "权重值必须在 0-1 之间");
            }
            sum += v;
        }
        if (Math.abs(sum - 1.0) > SUM_TOLERANCE) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "六维权重之和必须为 1(容差 0.001)");
        }
        if (request.getMinConfidence() != null
                && (request.getMinConfidence() < 0 || request.getMinConfidence() > 1)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "minConfidence 必须在 0-1 之间");
        }
        if (request.getTopN() != null && (request.getTopN() < 3 || request.getTopN() > 5)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "topN 必须在 3-5 之间");
        }
    }

    private double require(Double value, String field) {
        if (value == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, field + " 必填");
        }
        return value;
    }

    private WeightConfigVO toVO(RecommendationWeight weight) {
        WeightConfigVO vo = new WeightConfigVO();
        vo.setVersion(weight.getVersion());
        vo.setWeights(JsonUtil.parse(weight.getWeightsJson(), new TypeReference<WeightConfigVO.Weights>() {
        }));
        vo.setMinConfidence(weight.getMinConfidence() == null ? null : weight.getMinConfidence().doubleValue());
        vo.setTopN(weight.getTopN());
        vo.setStatus(weight.getStatus());
        vo.setPublishedAt(weight.getPublishedAt());
        return vo;
    }
}
