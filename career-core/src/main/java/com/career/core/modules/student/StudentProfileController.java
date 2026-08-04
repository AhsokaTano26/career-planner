package com.career.core.modules.student;

import com.career.core.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 接口1：获取学生画像（按线上 Apifox 定义调整）
 * GET /api/v1/profiles/latest?studentId={studentId}
 * 线上该接口无入参（依赖登录态确定当前用户）；Demo 无鉴权，studentId 作为可选增强参数，缺省取默认学生。
 * 学生不存在或画像未生成时返回空对象，HTTP 200。
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class StudentProfileController {

    /** Demo 无登录态时的默认学生ID（线上接口由 token 确定当前用户） */
    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) {
        this.service = service;
    }

    @GetMapping("/latest")
    public ApiResponse<Map<String, Object>> getLatestProfile(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return ApiResponse.success(service.getProfile(sid));
    }
}
