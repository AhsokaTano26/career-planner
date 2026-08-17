package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.ExportRequest;
import com.rickgao.careercore.modules.admin.service.AdminExportService;
import com.rickgao.careercore.modules.admin.vo.ExportJobVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端-数据导出接口。openapi:/api/v1/admin/exports。 */
@RestController
@RequestMapping("/api/v1/admin/exports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExportController {

    private static final String CREATE_ENDPOINT = "/api/v1/admin/exports";

    private final AdminExportService adminExportService;

    public AdminExportController(AdminExportService adminExportService) {
        this.adminExportService = adminExportService;
    }

    @GetMapping
    public ApiResponse<PageResult<ExportJobVO>> listExports(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminExportService.listExports(page, size, sort));
    }

    @PostMapping
    public ApiResponse<ExportJobVO> createExport(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ExportRequest request) {
        return ApiResponse.ok(adminExportService.createExport(
                SecurityUtils.currentUserId(), CREATE_ENDPOINT, idempotencyKey, request));
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<Resource> download(@PathVariable String jobId) {
        AdminExportService.DownloadFile download = adminExportService.download(jobId);
        FileSystemResource resource = new FileSystemResource(download.path());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
