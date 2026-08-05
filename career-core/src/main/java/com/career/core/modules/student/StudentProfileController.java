package com.career.core.modules.student;

import com.career.core.common.ApiResponse;
import com.career.core.modules.profile.ProfileFeedbackRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 学生画像公开接口。
 *
 * <p>正式路径与后端负责人统一后的 Apifox 契约一致。Demo 尚无 JWT 登录态，
 * 因此保留可选 studentId（缺省 1001）；接入 Spring Security 后应由
 * CurrentUser 取得学生ID并移除该查询参数。</p>
 */
@RestController
@RequestMapping("/api/v1")
public class StudentProfileController {

    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) {
        this.service = service;
    }

    @GetMapping("/students/me/profile/latest")
    public ApiResponse<Map<String, Object>> getLatestProfile(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return ApiResponse.success(service.getLatestProfile(resolveStudentId(studentId)));
    }

    @GetMapping("/students/me/profile/versions")
    public ApiResponse<Map<String, Object>> getProfileVersions(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return ApiResponse.success(service.getProfileVersions(resolveStudentId(studentId), page, size));
    }

    @GetMapping("/profile-snapshots/{snapshotId}")
    public ApiResponse<Map<String, Object>> getProfileSnapshot(
            @PathVariable Long snapshotId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return ApiResponse.success(
                service.getProfileSnapshot(snapshotId, resolveStudentId(studentId)));
    }

    @PostMapping("/students/me/profile/refresh")
    public ApiResponse<Map<String, Object>> refreshProfile(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // Demo 精简点：当前接收但尚未持久化幂等键；接入认证模块时统一由幂等组件处理。
        return ApiResponse.success(service.refreshProfile(resolveStudentId(studentId)));
    }

    @PostMapping("/profile-snapshots/{snapshotId}/feedback")
    public ApiResponse<Map<String, Object>> addProfileFeedback(
            @PathVariable Long snapshotId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) ProfileFeedbackRequest request) {
        service.addFeedback(snapshotId, resolveStudentId(studentId), request);
        return ApiResponse.success(Collections.emptyMap());
    }

    /**
     * 兼容第一版 Demo/旧冒烟脚本；新调用方应使用 /students/me/profile/latest。
     */
    @Deprecated
    @GetMapping("/profiles/latest")
    public ApiResponse<Map<String, Object>> getLegacyLatestProfile(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        return ApiResponse.success(service.getLatestProfile(resolveStudentId(studentId)));
    }

    private long resolveStudentId(Long studentId) {
        return studentId == null ? DEFAULT_STUDENT_ID : studentId;
    }
}
