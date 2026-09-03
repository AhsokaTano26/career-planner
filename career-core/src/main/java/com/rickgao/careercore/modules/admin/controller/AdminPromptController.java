package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.PromptVersionRequest;
import com.rickgao.careercore.modules.admin.service.AdminPromptService;
import com.rickgao.careercore.modules.admin.vo.PromptVersionVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-提示词版本接口。openapi:/api/v1/admin/prompts。
 */
@RestController
@RequestMapping("/api/v1/admin/prompts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromptController {

    private final AdminPromptService service;

    public AdminPromptController(AdminPromptService service) {
        this.service = service;
    }

    @GetMapping("/scenes")
    public ApiResponse<List<String>> listScenes() {
        return ApiResponse.ok(service.listScenes());
    }

    @GetMapping
    public ApiResponse<List<PromptVersionVO>> listVersions(
            @RequestParam(required = false) String scene) {
        return ApiResponse.ok(service.listVersions(scene));
    }

    @PostMapping
    public ApiResponse<PromptVersionVO> createVersion(@Valid @RequestBody PromptVersionRequest req) {
        return ApiResponse.ok(service.createVersion(SecurityUtils.currentUserId(), req));
    }

    @PostMapping("/{promptId}/publish")
    public ApiResponse<PromptVersionVO> publishVersion(@PathVariable String promptId) {
        return ApiResponse.ok(service.publishVersion(SecurityUtils.currentUserId(), promptId));
    }
}
