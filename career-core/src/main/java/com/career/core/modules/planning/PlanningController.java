package com.career.core.modules.planning;

import com.career.core.common.NotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 计划模块（按线上 Apifox「目标计划」定义）。
 * <p>
 * 契约对齐说明（2026-08-15）：线上 200 响应 schema 为裸业务对象（无 {code,message,data} 包装），
 * 故成功响应直接返回业务对象；资源不存在时返回 HTTP 404。
 * 线上接口依赖登录态确定当前学生；Demo 无鉴权，统一以可选 studentId 增强参数缺省 1001。
 */
@RestController
@RequestMapping("/api/v1")
public class PlanningController {

    /** Demo 无登录态时的默认学生ID */
    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final PlanningService service;

    public PlanningController(PlanningService service) {
        this.service = service;
    }

    /** 生成计划草案：POST /students/me/plans/draft */
    @PostMapping("/students/me/plans/draft")
    public PlanDraftDto generateDraft(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String directionId = body == null ? null : (String) body.get("directionId");
        boolean useAi = body != null && Boolean.TRUE.equals(body.get("useAi"));
        return service.generatePlanDraft(sid, directionId, useAi);
    }

    /** 最新计划：GET /students/me/plans/latest */
    @GetMapping("/students/me/plans/latest")
    public PlanDto getLatestPlan(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        PlanDto plan = service.getLatestPlan(sid);
        if (plan == null) {
            throw new NotFoundException("暂无计划，请先生成计划草案");
        }
        return plan;
    }

    /** 编辑计划：PATCH /plans/{planId} */
    @PatchMapping("/plans/{planId}")
    public PlanDto editPlan(@PathVariable Long planId, @RequestBody(required = false) Map<String, Object> body) {
        String semester = body == null ? null : (String) body.get("semester");
        return service.editPlan(planId, semester);
    }

    /** 确认计划：POST /plans/{planId}/confirm */
    @PostMapping("/plans/{planId}/confirm")
    public PlanDto confirmPlan(@PathVariable Long planId) {
        return service.confirmPlan(planId);
    }

    /** 计划版本历史：GET /plan-versions */
    @GetMapping("/plan-versions")
    public List<PlanDto> getPlanVersions(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.getPlanVersions(sid);
    }

    /** 我的目标：GET /students/me/goals */
    @GetMapping("/students/me/goals")
    public List<GoalDto> getGoals(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.getGoals(sid);
    }

    /** 设置 / 变更目标：POST /students/me/goals */
    @PostMapping("/students/me/goals")
    public GoalDto setGoal(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String directionId = body == null ? null : (String) body.get("directionId");
        String title = body == null ? null : (String) body.get("title");
        String goalType = body == null ? null : (String) body.get("goalType");
        return service.setGoal(sid, directionId, title, goalType);
    }

    /** 目标版本历史：GET /goal-versions */
    @GetMapping("/goal-versions")
    public List<GoalDto> getGoalVersions(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.getGoalVersions(sid);
    }

    /** 任务列表：GET /tasks */
    @GetMapping("/tasks")
    public List<TaskDto> getTasks(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.getTasks(sid);
    }

    /** 新增任务：POST /tasks */
    @PostMapping("/tasks")
    public TaskDto addTask(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String title = body == null ? null : (String) body.get("title");
        String month = body == null ? null : (String) body.get("month");
        return service.addTask(sid, title, month);
    }

    /** 更新任务：PATCH /tasks/{taskId} */
    @PatchMapping("/tasks/{taskId}")
    public TaskDto updateTask(@PathVariable Long taskId, @RequestBody(required = false) Map<String, Object> body) {
        String title = body == null ? null : (String) body.get("title");
        String status = body == null ? null : (String) body.get("status");
        return service.updateTask(taskId, title, status);
    }

    /** 删除任务：DELETE /tasks/{taskId} */
    @DeleteMapping("/tasks/{taskId}")
    public Map<String, Object> deleteTask(@PathVariable Long taskId) {
        service.deleteTask(taskId);
        return Map.of();
    }

    /** 任务打卡：POST /tasks/{taskId}/checkin */
    @PostMapping("/tasks/{taskId}/checkin")
    public TaskDto checkinTask(@PathVariable Long taskId) {
        return service.checkinTask(taskId);
    }

    /** 站内提醒：GET /students/me/reminders */
    @GetMapping("/students/me/reminders")
    public List<Map<String, Object>> getReminders(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.getReminders(sid);
    }
}

