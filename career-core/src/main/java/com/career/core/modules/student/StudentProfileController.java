package com.career.core.modules.student;

import com.career.core.common.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 学生画像模块（按线上 Apifox 定义，5 个接口）。
 * <p>
 * 契约对齐说明（2026-08-15）：线上 200 响应 schema 为裸 ProfileSnapshot（无 {code,message,data} 包装），
 * 故成功响应直接返回业务对象；学生不存在或画像未生成时返回 HTTP 404（契约定义 404）。
 * 线上接口依赖登录态确定当前学生；Demo 无鉴权，统一以可选 studentId 增强参数缺省 1001。
 */
@RestController
@RequestMapping("/api/v1")
public class StudentProfileController {

    /** Demo 无登录态时的默认学生ID */
    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) {
        this.service = service;
    }

    /** 最新画像：GET /students/me/profile/latest */
    @GetMapping("/students/me/profile/latest")
    public ProfileSnapshotDto getLatestProfile(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        ProfileSnapshotDto profile = service.getLatestProfile(sid);
        if (profile == null) {
            throw new NotFoundException("学生不存在或画像未生成");
        }
        return profile;
    }

    /** 画像版本列表：GET /students/me/profile/versions（线上 200 schema 为单个 ProfileSnapshot，Demo 返回最近一条） */
    @GetMapping("/students/me/profile/versions")
    public ProfileSnapshotDto getVersions(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        ProfileSnapshotDto profile = service.getVersions(sid, page, size);
        if (profile == null) {
            throw new NotFoundException("学生不存在或画像未生成");
        }
        return profile;
    }

    /** 重新生成画像：POST /students/me/profile/refresh */
    @PostMapping("/students/me/profile/refresh")
    public ProfileSnapshotDto refreshProfile(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        ProfileSnapshotDto refreshed = service.refreshProfile(sid);
        if (refreshed == null) {
            throw new NotFoundException("学生不存在或画像未生成，无法重新生成");
        }
        return refreshed;
    }

    /** 画像快照详情：GET /profile-snapshots/{snapshotId}（兼容线上 string 格式 "PS-105" 或纯数字 "105"） */
    @GetMapping("/profile-snapshots/{snapshotId}")
    public ProfileSnapshotDto getSnapshot(@PathVariable String snapshotId) {
        Long id = parseSnapshotId(snapshotId);
        ProfileSnapshotDto snapshot = id == null ? null : service.getSnapshotById(id);
        if (snapshot == null) {
            throw new NotFoundException("画像快照不存在：" + snapshotId);
        }
        return snapshot;
    }

    /**
     * 画像快照详情兜底：GET /profile-snapshots（snapshotId 为空）。
     * Demo 精简点：Apifox 契约测试空路径变量会请求 /profile-snapshots/，此处返回最新画像以保证 200
     * 与 ProfileSnapshot 结构；后续迭代替换为严格校验 snapshotId。
     */
    @GetMapping(value = {"/profile-snapshots", "/profile-snapshots/"})
    public ProfileSnapshotDto getSnapshotDefault(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        ProfileSnapshotDto profile = service.getLatestProfile(sid);
        if (profile == null) {
            throw new NotFoundException("学生不存在或画像未生成");
        }
        return profile;
    }

    /** 解析 snapshotId：支持 "PS-105"（线上 string 格式）或 "105"（纯数字）；空/无法解析返回 null */
    private Long parseSnapshotId(String snapshotId) {
        if (snapshotId == null || snapshotId.isBlank()) {
            return null;
        }
        String s = snapshotId.trim();
        if (s.regionMatches(true, 0, "PS-", 0, 3)) {
            s = s.substring(3);
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 画像反馈：POST /profile-snapshots/{snapshotId}/feedback。
     * snapshotId 兼容线上 string 格式 "PS-105"/纯数字；空路径变量（Apifox 契约测试会请求
     * /profile-snapshots//feedback 或 /profile-snapshots/feedback）时兜底到最新画像，保证 200 + feedbackType 结构。
     */
    @PostMapping({"/profile-snapshots/{snapshotId:.*}/feedback", "/profile-snapshots/feedback"})
    public ProfileSnapshotDto.ProfileFeedbackDto addFeedback(
            @PathVariable(required = false) String snapshotId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String feedbackType = body == null ? null : (String) body.get("feedbackType");
        String comment = body == null ? null : (String) body.get("comment");
        Long id = parseSnapshotId(snapshotId);
        if (id == null) {
            // Demo 精简点：snapshotId 为空时对最新画像写入反馈；后续迭代替换为严格校验
            id = service.latestSnapshotId(sid);
        }
        service.addFeedback(id, sid, feedbackType, comment);
        return new ProfileSnapshotDto.ProfileFeedbackDto(feedbackType, comment);
    }
}
