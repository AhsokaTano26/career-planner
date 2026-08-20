package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.AbilityTagRequest;
import com.rickgao.careercore.modules.admin.service.AdminAbilityService;
import com.rickgao.careercore.modules.admin.vo.AbilityTagVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端-能力标签接口。openapi:/api/v1/admin/abilities。 */
@RestController
@RequestMapping("/api/v1/admin/abilities")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAbilityController {

    private static final String CREATE_ENDPOINT = "/api/v1/admin/abilities";
    private static final String UPDATE_ENDPOINT = "/api/v1/admin/abilities/{tagId}";

    private final AdminAbilityService adminAbilityService;

    public AdminAbilityController(AdminAbilityService adminAbilityService) {
        this.adminAbilityService = adminAbilityService;
    }

    @GetMapping
    public ApiResponse<PageResult<AbilityTagVO>> listAbilities(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminAbilityService.listAbilities(category, keyword, page, size, sort));
    }

    @PostMapping
    public ApiResponse<AbilityTagVO> createAbility(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AbilityTagRequest request) {
        return ApiResponse.ok(adminAbilityService.createAbility(
                SecurityUtils.currentUserId(), CREATE_ENDPOINT, idempotencyKey, request));
    }

    @PatchMapping("/{tagId}")
    public ApiResponse<AbilityTagVO> updateAbility(
            @PathVariable String tagId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AbilityTagRequest request) {
        return ApiResponse.ok(adminAbilityService.updateAbility(
                SecurityUtils.currentUserId(), UPDATE_ENDPOINT, idempotencyKey, tagId, request));
    }
}
