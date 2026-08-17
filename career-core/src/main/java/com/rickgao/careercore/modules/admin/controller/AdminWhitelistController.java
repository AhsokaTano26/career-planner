package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.WhitelistCreate;
import com.rickgao.careercore.modules.admin.service.AdminWhitelistService;
import com.rickgao.careercore.modules.admin.vo.WhitelistEntryVO;
import com.rickgao.careercore.modules.admin.vo.WhitelistImportResultVO;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端-白名单接口。
 * openapi:GET/POST /api/v1/admin/whitelist、POST /whitelist/import、DELETE /whitelist/{whitelistId}。
 */
@RestController
@RequestMapping("/api/v1/admin/whitelist")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWhitelistController {

    private static final String CREATE_ENDPOINT = "/api/v1/admin/whitelist";
    private static final String IMPORT_ENDPOINT = "/api/v1/admin/whitelist/import";
    private static final String DELETE_ENDPOINT = "/api/v1/admin/whitelist/{whitelistId}";

    private final AdminWhitelistService adminWhitelistService;

    public AdminWhitelistController(AdminWhitelistService adminWhitelistService) {
        this.adminWhitelistService = adminWhitelistService;
    }

    @GetMapping
    public ApiResponse<PageResult<WhitelistEntryVO>> listWhitelist(
            @RequestParam(required = false) Boolean used,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminWhitelistService.listWhitelist(used, keyword, page, size, sort));
    }

    @PostMapping
    public ApiResponse<WhitelistEntryVO> createWhitelist(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WhitelistCreate dto) {
        return ApiResponse.ok(adminWhitelistService.createWhitelist(
                SecurityUtils.currentUserId(), CREATE_ENDPOINT, idempotencyKey, dto));
    }

    @PostMapping("/import")
    public ApiResponse<WhitelistImportResultVO> importWhitelist(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(adminWhitelistService.importWhitelist(
                SecurityUtils.currentUserId(), IMPORT_ENDPOINT, idempotencyKey, file));
    }

    @DeleteMapping("/{whitelistId}")
    public ApiResponse<Void> deleteWhitelist(
            @PathVariable String whitelistId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        adminWhitelistService.deleteWhitelist(
                SecurityUtils.currentUserId(), DELETE_ENDPOINT, idempotencyKey, whitelistId);
        return ApiResponse.ok();
    }
}
