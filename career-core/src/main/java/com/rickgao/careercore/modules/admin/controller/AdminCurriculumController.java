package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.BatchReviewRequest;
import com.rickgao.careercore.modules.admin.dto.CurriculumPublishRequest;
import com.rickgao.careercore.modules.admin.dto.ImportItemUpdate;
import com.rickgao.careercore.modules.admin.service.AdminCurriculumService;
import com.rickgao.careercore.modules.admin.vo.CurriculumImportJobVO;
import com.rickgao.careercore.modules.admin.vo.CurriculumVersionVO;
import com.rickgao.careercore.modules.admin.vo.ImportItemVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 管理端-培养方案接口。openapi:/api/v1/admin/curricula/*。 */
@RestController
@RequestMapping("/api/v1/admin/curricula")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCurriculumController {

    private static final String IMPORT_ENDPOINT = "/api/v1/admin/curricula/import";
    private static final String ITEM_ENDPOINT = "/api/v1/admin/curricula/items/{itemId}";
    private static final String BATCH_ENDPOINT = "/api/v1/admin/curricula/items/batch";
    private static final String PUBLISH_ENDPOINT = "/api/v1/admin/curricula/publish";

    private final AdminCurriculumService adminCurriculumService;

    public AdminCurriculumController(AdminCurriculumService adminCurriculumService) {
        this.adminCurriculumService = adminCurriculumService;
    }

    /** 上传培养方案 PDF / Word,创建解析任务(202,前端轮询任务详情) */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<CurriculumImportJobVO>> importFile(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestParam("file") MultipartFile file) {
        CurriculumImportJobVO job = adminCurriculumService.importFile(
                SecurityUtils.currentUserId(), IMPORT_ENDPOINT, idempotencyKey, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(job));
    }

    @GetMapping("/jobs")
    public ApiResponse<PageResult<CurriculumImportJobVO>> listJobs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminCurriculumService.listJobs(page, size, sort));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<CurriculumImportJobVO> getJob(@PathVariable String jobId) {
        return ApiResponse.ok(adminCurriculumService.getJob(jobId));
    }

    @GetMapping("/items")
    public ApiResponse<PageResult<ImportItemVO>> listItems(
            @RequestParam String jobId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminCurriculumService.listItems(jobId, status, page, size, sort));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<ImportItemVO> reviewItem(
            @PathVariable String itemId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ImportItemUpdate update) {
        return ApiResponse.ok(adminCurriculumService.reviewItem(
                SecurityUtils.currentUserId(), ITEM_ENDPOINT, idempotencyKey, itemId, update));
    }

    @PostMapping("/items/batch")
    public ApiResponse<List<ImportItemVO>> batchReview(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BatchReviewRequest request) {
        return ApiResponse.ok(adminCurriculumService.batchReview(
                SecurityUtils.currentUserId(), BATCH_ENDPOINT, idempotencyKey, request));
    }

    @GetMapping("/versions")
    public ApiResponse<PageResult<CurriculumVersionVO>> listVersions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(adminCurriculumService.listVersions(page, size, sort));
    }

    @PostMapping("/publish")
    public ApiResponse<CurriculumVersionVO> publish(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CurriculumPublishRequest request) {
        return ApiResponse.ok(adminCurriculumService.publish(
                SecurityUtils.currentUserId(), PUBLISH_ENDPOINT, idempotencyKey, request));
    }
}
