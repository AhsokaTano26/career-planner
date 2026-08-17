package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.WeightConfigRequest;
import com.rickgao.careercore.modules.admin.service.AdminWeightService;
import com.rickgao.careercore.modules.admin.vo.WeightConfigVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端-推荐权重接口。openapi:/api/v1/admin/weights。 */
@RestController
@RequestMapping("/api/v1/admin/weights")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWeightController {

    private static final String CREATE_ENDPOINT = "/api/v1/admin/weights";

    private final AdminWeightService adminWeightService;

    public AdminWeightController(AdminWeightService adminWeightService) {
        this.adminWeightService = adminWeightService;
    }

    @GetMapping
    public ApiResponse<WeightConfigVO> getWeights() {
        return ApiResponse.ok(adminWeightService.getWeights());
    }

    @PostMapping
    public ApiResponse<WeightConfigVO> createWeights(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WeightConfigRequest request) {
        return ApiResponse.ok(adminWeightService.createWeights(
                SecurityUtils.currentUserId(), CREATE_ENDPOINT, idempotencyKey, request));
    }
}
