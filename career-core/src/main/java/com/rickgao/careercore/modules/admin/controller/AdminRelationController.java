package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.RelationRequest;
import com.rickgao.careercore.modules.admin.service.AdminRelationService;
import com.rickgao.careercore.modules.admin.vo.AdvisorRelationVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-辅导员学生关系接口。
 * openapi:GET/POST /api/v1/admin/relations、DELETE /api/v1/admin/relations/{relationId}。
 */
@RestController
@RequestMapping("/api/v1/admin/relations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRelationController {

    private static final String CREATE_ENDPOINT = "/api/v1/admin/relations";
    private static final String DELETE_ENDPOINT = "/api/v1/admin/relations/{relationId}";

    private final AdminRelationService adminRelationService;

    public AdminRelationController(AdminRelationService adminRelationService) {
        this.adminRelationService = adminRelationService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdvisorRelationVO>> listRelations(
            @RequestParam(required = false) String advisorId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminRelationService.listRelations(advisorId, page, size, sort));
    }

    @PostMapping
    public ApiResponse<List<AdvisorRelationVO>> createRelations(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RelationRequest request) {
        return ApiResponse.ok(adminRelationService.createRelations(
                SecurityUtils.currentUserId(), CREATE_ENDPOINT, idempotencyKey, request));
    }

    @DeleteMapping("/{relationId}")
    public ApiResponse<Void> deleteRelation(
            @PathVariable String relationId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        adminRelationService.deleteRelation(
                SecurityUtils.currentUserId(), DELETE_ENDPOINT, idempotencyKey, relationId);
        return ApiResponse.ok();
    }
}
