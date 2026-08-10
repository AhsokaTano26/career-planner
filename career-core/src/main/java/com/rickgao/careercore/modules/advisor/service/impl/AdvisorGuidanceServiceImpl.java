package com.rickgao.careercore.modules.advisor.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.advisor.dto.GuidanceCommentRequest;
import com.rickgao.careercore.modules.advisor.entity.AdvisorComment;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorCommentMapper;
import com.rickgao.careercore.modules.advisor.service.AdvisorGuidanceService;
import com.rickgao.careercore.modules.advisor.service.AdvisorScopeService;
import com.rickgao.careercore.modules.advisor.vo.GuidanceCommentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 指导意见/建议服务实现。
 * 写入规则:COMMENT 无附加必填;SUGGEST_TASK 必填 suggestedTask;SUGGEST_RETEST 必填 retestReason。
 * POST /guidance 与 POST /advice 共用本服务,仅写 advisor_comment,不覆盖学生原始数据。
 */
@Service
public class AdvisorGuidanceServiceImpl implements AdvisorGuidanceService {

    private static final Set<String> ADVICE_TYPES = Set.of("COMMENT", "SUGGEST_TASK", "SUGGEST_RETEST");

    private final AdvisorScopeService advisorScopeService;
    private final AdvisorCommentMapper advisorCommentMapper;
    private final IdGenerator idGenerator;
    private final IdempotencyService idempotencyService;

    public AdvisorGuidanceServiceImpl(AdvisorScopeService advisorScopeService,
                                      AdvisorCommentMapper advisorCommentMapper,
                                      IdGenerator idGenerator,
                                      IdempotencyService idempotencyService) {
        this.advisorScopeService = advisorScopeService;
        this.advisorCommentMapper = advisorCommentMapper;
        this.idGenerator = idGenerator;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public List<GuidanceCommentVO> listGuidance(String advisorId, String studentId) {
        advisorScopeService.assertAssigned(advisorId, studentId);
        return advisorCommentMapper.findByStudentId(studentId).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public GuidanceCommentVO writeGuidance(String advisorId, String studentId, String endpoint, String idempotencyKey,
                                           GuidanceCommentRequest request) {
        advisorScopeService.assertAssigned(advisorId, studentId);
        validate(request);
        ApiResponse<GuidanceCommentVO> response = idempotencyService.execute(
                advisorId, endpoint, idempotencyKey,
                () -> {
                    AdvisorComment comment = new AdvisorComment();
                    comment.setId(idGenerator.advisorCommentId());
                    comment.setStudentId(studentId);
                    comment.setAdvisorId(advisorId);
                    comment.setContent(request.getContent().trim());
                    comment.setAdviceType(request.getAdviceType());
                    comment.setSuggestedTask(trimToNull(request.getSuggestedTask()));
                    comment.setRetestReason(trimToNull(request.getRetestReason()));
                    advisorCommentMapper.insert(comment);
                    return ApiResponse.ok(toVO(comment));
                });
        return response.getData();
    }

    private void validate(GuidanceCommentRequest request) {
        String type = request.getAdviceType();
        if (!ADVICE_TYPES.contains(type)) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "adviceType 仅支持 COMMENT/SUGGEST_TASK/SUGGEST_RETEST");
        }
        if ("SUGGEST_TASK".equals(type) && !StringUtils.hasText(request.getSuggestedTask())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "adviceType=SUGGEST_TASK 时 suggestedTask 必填");
        }
        if ("SUGGEST_RETEST".equals(type) && !StringUtils.hasText(request.getRetestReason())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "adviceType=SUGGEST_RETEST 时 retestReason 必填");
        }
    }

    private GuidanceCommentVO toVO(AdvisorComment comment) {
        GuidanceCommentVO vo = new GuidanceCommentVO();
        vo.setId(comment.getId());
        vo.setStudentId(comment.getStudentId());
        vo.setContent(comment.getContent());
        vo.setAdviceType(comment.getAdviceType());
        vo.setSuggestedTask(comment.getSuggestedTask());
        vo.setRetestReason(comment.getRetestReason());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
