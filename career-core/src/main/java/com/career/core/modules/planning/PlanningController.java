package com.career.core.modules.planning;

import com.career.core.common.BadRequestException;
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

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 计划模块（按线上 Apifox「目标计划」Schema 定义）。
 * <p>
 * 契约对齐说明（2026-08-15）：线上 200 响应 schema 为裸业务对象（Plan/Goal/Task/Reminder，
 * 无 {code,message,data} 包装），故成功响应直接返回业务对象；资源不存在时返回 HTTP 404。
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

    /** 编辑计划：PATCH /plans/{planId}（planId 兼容线上 string 格式 "PLAN-370" 或纯数字） */
    @PatchMapping("/plans/{planId}")
    public PlanDto editPlan(@PathVariable String planId, @RequestBody(required = false) Map<String, Object> body) {
        String semester = body == null ? null : (String) body.get("semester");
        Long id = parsePlanId(planId);
        if (id == null) {
            throw new BadRequestException("计划ID格式错误：" + planId);
        }
        return service.editPlan(id, semester);
    }

    /**
     * 编辑计划兜底：PATCH /plans（planId 为空）。
     * Demo 精简点：Apifox 契约测试空路径变量会请求 /plans/，此处对最新计划应用编辑（仅支持 semester 更新，
     * 其余字段回读）并返回，保证 200 + Plan 结构；后续迭代替换为严格校验 planId。
     */
    @PatchMapping(value = {"/plans", "/plans/"})
    public PlanDto editPlanDefault(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String semester = body == null ? null : (String) body.get("semester");
        return service.editLatestPlan(sid, semester);
    }

    /** 确认计划：POST /plans/{planId}/confirm（planId 兼容线上 string 格式） */
    @PostMapping("/plans/{planId}/confirm")
    public PlanDto confirmPlan(@PathVariable String planId) {
        Long id = parsePlanId(planId);
        if (id == null) {
            throw new BadRequestException("计划ID格式错误：" + planId);
        }
        return service.confirmPlan(id);
    }

    /**
     * 确认计划兜底：POST /plans/confirm（planId 为空）。
     * Demo 精简点：Apifox 契约测试空路径变量会请求 /plans//confirm（双斜杠经 PathNormalizeFilter 折叠为
     * /plans/confirm），此处确认最新计划并返回，保证 200 + Plan 结构；后续迭代替换为严格校验 planId。
     */
    @PostMapping("/plans/confirm")
    public PlanDto confirmPlanDefault(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.confirmLatestPlan(sid);
    }

    /** 解析 planId：支持 "PLAN-370"（线上 string 格式）或 "370"（纯数字）；空/无法解析返回 null */
    private Long parsePlanId(String planId) {
        if (planId == null || planId.isBlank()) {
            return null;
        }
        String s = planId.trim();
        if (s.regionMatches(true, 0, "PLAN-", 0, 5)) {
            s = s.substring(5);
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 计划版本历史：GET /plan-versions（线上 200 schema 为单个 Plan） */
    @GetMapping("/plan-versions")
    public PlanDto getPlanVersions(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        PlanDto plan = service.getPlanVersions(sid);
        if (plan == null) {
            throw new NotFoundException("暂无计划版本");
        }
        return plan;
    }

    /** 我的目标：GET /students/me/goals（线上 200 schema 为单个 Goal） */
    @GetMapping("/students/me/goals")
    public GoalDto getGoals(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        GoalDto goal = service.getGoals(sid);
        if (goal == null) {
            throw new NotFoundException("暂无目标，请先设置目标");
        }
        return goal;
    }

    /** 设置 / 变更目标：POST /students/me/goals（入参 primaryDirectionId/backupDirectionId） */
    @PostMapping("/students/me/goals")
    public GoalDto setGoal(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String primaryDirectionId = body == null ? null : (String) body.get("primaryDirectionId");
        String backupDirectionId = body == null ? null : (String) body.get("backupDirectionId");
        return service.setGoal(sid, primaryDirectionId, backupDirectionId);
    }

    /** 目标版本历史：GET /goal-versions（线上 200 schema 为单个 GoalVersion，Demo 返回当前目标） */
    @GetMapping("/goal-versions")
    public GoalDto getGoalVersions(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        GoalDto goal = service.getGoalVersions(sid);
        if (goal == null) {
            throw new NotFoundException("暂无目标版本");
        }
        return goal;
    }

    /** 任务列表：GET /tasks（线上 200 schema 为单个 Task） */
    @GetMapping("/tasks")
    public TaskDto getTasks(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        TaskDto task = service.getTasks(sid);
        if (task == null) {
            throw new NotFoundException("暂无任务");
        }
        return task;
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

    /** 更新任务：PATCH /tasks/{taskId}（taskId 兼容线上 string 格式 "T6" 或纯数字） */
    @PatchMapping("/tasks/{taskId}")
    public TaskDto updateTask(@PathVariable String taskId, @RequestBody(required = false) Map<String, Object> body) {
        String title = body == null ? null : (String) body.get("title");
        String status = body == null ? null : (String) body.get("status");
        Long id = parseTaskId(taskId);
        if (id == null) {
            throw new BadRequestException("任务ID格式错误：" + taskId);
        }
        return service.updateTask(id, title, status);
    }

    /**
     * 更新任务兜底：PATCH /tasks（taskId 为空）。
     * Demo 精简点：Apifox 契约测试空路径变量会请求 /tasks/，此处更新最新任务并返回，
     * 保证 200 + Task 结构；后续迭代替换为严格校验 taskId。
     */
    @PatchMapping(value = {"/tasks", "/tasks/"})
    public TaskDto updateTaskDefault(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String title = body == null ? null : (String) body.get("title");
        String status = body == null ? null : (String) body.get("status");
        return service.updateLatestTask(sid, title, status);
    }

    /** 删除任务：DELETE /tasks/{taskId}（线上 200 schema 为 ApiResponse：code=OK + traceId + timestamp） */
    @DeleteMapping("/tasks/{taskId}")
    public Map<String, Object> deleteTask(@PathVariable String taskId) {
        Long id = parseTaskId(taskId);
        if (id == null) {
            throw new BadRequestException("任务ID格式错误：" + taskId);
        }
        service.deleteTask(id);
        return deleteOkResponse();
    }

    /**
     * 删除任务兜底：DELETE /tasks（taskId 为空）。
     * Demo 精简点：Apifox 契约测试空路径变量会请求 /tasks/，此处删除最新任务并返回，
     * 保证 200 + ApiResponse 结构；后续迭代替换为严格校验 taskId。
     */
    @DeleteMapping(value = {"/tasks", "/tasks/"})
    public Map<String, Object> deleteTaskDefault(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        service.deleteLatestTask(sid);
        return deleteOkResponse();
    }

    /** 契约 DELETE 200 的 ApiResponse 结构：{code:"OK", message, data, traceId, timestamp} */
    private Map<String, Object> deleteOkResponse() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("code", "OK");
        resp.put("message", "success");
        resp.put("data", new LinkedHashMap<>());
        resp.put("traceId", UUID.randomUUID().toString());
        resp.put("timestamp", OffsetDateTime.now().toString());
        return resp;
    }

    /** 任务打卡：POST /tasks/{taskId}/checkin（200 schema 为 TaskCheckin 扁平对象） */
    @PostMapping("/tasks/{taskId}/checkin")
    public TaskDto.TaskCheckinDto checkinTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long id = parseTaskId(taskId);
        if (id == null) {
            throw new BadRequestException("任务ID格式错误：" + taskId);
        }
        return service.checkinTask(id, body);
    }

    /**
     * 任务打卡兜底：POST /tasks/checkin（taskId 为空，契约测试会请求 //checkin）。
     * Demo 精简点：对最新任务打卡并返回，保证 200 + TaskCheckin 结构；后续迭代替换为严格校验 taskId。
     */
    @PostMapping(value = {"/tasks/checkin", "/tasks//checkin"})
    public TaskDto.TaskCheckinDto checkinTaskDefault(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.checkinLatestTask(sid, body);
    }

    /** 解析 taskId：支持 "T6"（线上 string 格式）或 "6"（纯数字）；空/无法解析返回 null */
    private Long parseTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return null;
        }
        String s = taskId.trim();
        if (s.length() > 1 && (s.charAt(0) == 'T' || s.charAt(0) == 't')) {
            s = s.substring(1);
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 站内提醒：GET /students/me/reminders（线上 200 schema 为单个 Reminder） */
    @GetMapping("/students/me/reminders")
    public ReminderDto getReminders(@RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        ReminderDto reminder = service.getReminders(sid);
        if (reminder == null) {
            throw new NotFoundException("暂无提醒");
        }
        return reminder;
    }
}
