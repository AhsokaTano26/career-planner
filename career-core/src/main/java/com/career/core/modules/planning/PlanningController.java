package com.career.core.modules.planning;

import com.career.core.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 计划模块（按线上 Apifox 定义实现）
 * POST /api/v1/planning/plans/generate —— AI生成计划草案（调用推荐数据生成）
 * 线上无入参（依赖登录态）；Demo 无鉴权，studentId 为可选增强参数，缺省取默认学生。
 */
@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

    /** Demo 无登录态时的默认学生ID */
    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final PlanningService service;

    public PlanningController(PlanningService service) {
        this.service = service;
    }

    @PostMapping("/plans/generate")
    public ApiResponse<Map<String, Object>> generatePlan(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return ApiResponse.success(service.generatePlanDraft(sid));
    }
}
