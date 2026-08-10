package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.DirectionRequest;
import com.rickgao.careercore.modules.admin.dto.DirectionStatusUpdate;
import com.rickgao.careercore.modules.admin.service.AdminDirectionService;
import com.rickgao.careercore.modules.admin.vo.AdminDirectionVO;
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

/** 管理端-方向库接口。openapi:/api/v1/admin/directions。 */
@RestController
@RequestMapping("/api/v1/admin/directions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDirectionController {

    private static final String CREATE_ENDPOINT = "/api/v1/admin/directions";
    private static final String UPDATE_ENDPOINT = "/api/v1/admin/directions/{directionId}";
    private static final String STATUS_ENDPOINT = "/api/v1/admin/directions/{directionId}/status";

    private final AdminDirectionService adminDirectionService;

    public AdminDirectionController(AdminDirectionService adminDirectionService) {
        this.adminDirectionService = adminDirectionService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminDirectionVO>> listDirections(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminDirectionService.listDirections(path, status, keyword, page, size, sort));
    }

    @PostMapping
    public ApiResponse<AdminDirectionVO> createDirection(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DirectionRequest request) {
        return ApiResponse.ok(adminDirectionService.createDirection(
                SecurityUtils.currentUserId(), CREATE_ENDPOINT, idempotencyKey, request));
    }

    @PatchMapping("/{directionId}")
    public ApiResponse<AdminDirectionVO> updateDirection(
            @PathVariable String directionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DirectionRequest request) {
        return ApiResponse.ok(adminDirectionService.updateDirection(
                SecurityUtils.currentUserId(), UPDATE_ENDPOINT, idempotencyKey, directionId, request));
    }

    @PatchMapping("/{directionId}/status")
    public ApiResponse<AdminDirectionVO> setDirectionStatus(
            @PathVariable String directionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DirectionStatusUpdate update) {
        return ApiResponse.ok(adminDirectionService.setDirectionStatus(
                SecurityUtils.currentUserId(), STATUS_ENDPOINT, idempotencyKey, directionId, update));
    }
}
