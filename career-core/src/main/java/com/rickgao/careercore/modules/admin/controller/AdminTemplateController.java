package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.TaskTemplateRequest;
import com.rickgao.careercore.modules.admin.service.AdminTemplateService;
import com.rickgao.careercore.modules.admin.vo.TaskTemplateVO;
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

/** 管理端-任务模板接口。openapi:/api/v1/admin/templates。 */
@RestController
@RequestMapping("/api/v1/admin/templates")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTemplateController {

    private static final String CREATE_ENDPOINT = "/api/v1/admin/templates";
    private static final String UPDATE_ENDPOINT = "/api/v1/admin/templates/{templateId}";

    private final AdminTemplateService adminTemplateService;

    public AdminTemplateController(AdminTemplateService adminTemplateService) {
        this.adminTemplateService = adminTemplateService;
    }

    @GetMapping
    public ApiResponse<PageResult<TaskTemplateVO>> listTemplates(
            @RequestParam(required = false) String directionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminTemplateService.listTemplates(directionId, page, size, sort));
    }

    @PostMapping
    public ApiResponse<TaskTemplateVO> createTemplate(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TaskTemplateRequest request) {
        return ApiResponse.ok(adminTemplateService.createTemplate(
                SecurityUtils.currentUserId(), CREATE_ENDPOINT, idempotencyKey, request));
    }

    @PatchMapping("/{templateId}")
    public ApiResponse<TaskTemplateVO> updateTemplate(
            @PathVariable String templateId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TaskTemplateRequest request) {
        return ApiResponse.ok(adminTemplateService.updateTemplate(
                SecurityUtils.currentUserId(), UPDATE_ENDPOINT, idempotencyKey, templateId, request));
    }
}
