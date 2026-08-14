package com.career.core.modules.student;

import com.career.core.common.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生画像模块（按线上 Apifox 定义）。
 * GET /api/v1/students/me/profile/latest —— 最新画像
 * GET /api/v1/students/me/profile/versions —— 画像版本列表
 * 契约对齐说明（2026-08-15）：线上 200 响应 schema 为裸 ProfileSnapshot（无 {code,message,data} 包装），
 * 故成功响应直接返回业务对象；学生不存在或画像未生成时返回 HTTP 404（契约定义 404）。
 */
@RestController
@RequestMapping("/api/v1/students/me/profile")
public class StudentProfileController {

    /** Demo 无登录态时的默认学生ID */
    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) {
        this.service = service;
    }

    /** 最新画像 */
    @GetMapping("/latest")
    public ProfileSnapshotDto getLatestProfile(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        ProfileSnapshotDto profile = service.getLatestProfile(sid);
        if (profile == null) {
            throw new NotFoundException("学生不存在或画像未生成");
        }
        return profile;
    }

    /** 画像版本列表（Demo 补充实现，线上为准） */
    @GetMapping("/versions")
    public List<ProfileSnapshotDto> getVersions(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.getVersions(sid, page, size);
    }
}
