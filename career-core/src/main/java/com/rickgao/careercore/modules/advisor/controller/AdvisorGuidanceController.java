package com.rickgao.careercore.modules.advisor.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.advisor.dto.GuidanceCommentRequest;
import com.rickgao.careercore.modules.advisor.service.AdvisorGuidanceService;
import com.rickgao.careercore.modules.advisor.vo.GuidanceCommentVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 辅导员端-指导意见/建议接口。
 * openapi:GET/POST /api/v1/advisor/students/{studentId}/guidance、POST .../advice。
 */
@RestController
@RequestMapping("/api/v1/advisor/students/{studentId}")
@PreAuthorize("hasRole('ADVISOR')")
public class AdvisorGuidanceController {

    private static final String GUIDANCE_ENDPOINT = "/api/v1/advisor/students/{studentId}/guidance";
    private static final String ADVICE_ENDPOINT = "/api/v1/advisor/students/{studentId}/advice";

    private final AdvisorGuidanceService advisorGuidanceService;

    public AdvisorGuidanceController(AdvisorGuidanceService advisorGuidanceService) {
        this.advisorGuidanceService = advisorGuidanceService;
    }

    /** 指导记录(历史意见,时间正序) */
    @GetMapping("/guidance")
    public ApiResponse<List<GuidanceCommentVO>> listGuidance(@PathVariable String studentId) {
        return ApiResponse.ok(advisorGuidanceService.listGuidance(SecurityUtils.currentUserId(), studentId));
    }

    /** 填写指导意见(或建议任务/建议重新测评) */
    @PostMapping("/guidance")
    public ApiResponse<GuidanceCommentVO> writeGuidance(
            @PathVariable String studentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody GuidanceCommentRequest request) {
        return ApiResponse.ok(advisorGuidanceService.writeGuidance(
                SecurityUtils.currentUserId(), studentId, GUIDANCE_ENDPOINT, idempotencyKey, request));
    }

    /** 提出建议任务 / 建议重新测评 */
    @PostMapping("/advice")
    public ApiResponse<GuidanceCommentVO> writeAdvice(
            @PathVariable String studentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody GuidanceCommentRequest request) {
        return ApiResponse.ok(advisorGuidanceService.writeGuidance(
                SecurityUtils.currentUserId(), studentId, ADVICE_ENDPOINT, idempotencyKey, request));
    }
}
