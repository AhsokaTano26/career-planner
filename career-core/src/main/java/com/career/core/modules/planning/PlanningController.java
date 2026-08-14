package com.career.core.modules.planning;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 计划模块（按线上 Apifox 定义）。
 * POST /api/v1/students/me/plans/draft —— 生成计划草案（请求体 directionId/useAi/requestId）
 * 契约对齐说明（2026-08-15）：线上 200 响应 schema 为裸 PlanDraft（无 {code,message,data} 包装），
 * 故成功响应直接返回业务对象。
 */
@RestController
@RequestMapping("/api/v1/students/me/plans")
public class PlanningController {

    /** Demo 无登录态时的默认学生ID */
    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final PlanningService service;

    public PlanningController(PlanningService service) {
        this.service = service;
    }

    @PostMapping("/draft")
    public PlanDraftDto generateDraft(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String directionId = body == null ? null : (String) body.get("directionId");
        boolean useAi = body != null && Boolean.TRUE.equals(body.get("useAi"));
        return service.generatePlanDraft(sid, directionId, useAi);
    }
}
