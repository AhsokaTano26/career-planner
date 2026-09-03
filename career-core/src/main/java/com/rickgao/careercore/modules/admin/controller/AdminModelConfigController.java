package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.ModelConfigUpdateRequest;
import com.rickgao.careercore.modules.admin.service.AdminModelConfigService;
import com.rickgao.careercore.modules.admin.vo.ModelConfigVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-模型配置接口。openapi:/api/v1/admin/model-configs。
 */
@RestController
@RequestMapping("/api/v1/admin/model-configs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModelConfigController {

    private final AdminModelConfigService service;

    public AdminModelConfigController(AdminModelConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ModelConfigVO>> listConfigs() {
        return ApiResponse.ok(service.listConfigs());
    }

    @PutMapping("/{configKey}")
    public ApiResponse<ModelConfigVO> updateConfig(@PathVariable String configKey,
                                                   @Valid @RequestBody ModelConfigUpdateRequest req) {
        return ApiResponse.ok(service.updateConfig(SecurityUtils.currentUserId(), configKey, req));
    }
}
