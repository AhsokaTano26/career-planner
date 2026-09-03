package com.rickgao.careercore.modules.planning.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.advisor.vo.GoalVO;
import com.rickgao.careercore.modules.advisor.vo.PlanVO;
import com.rickgao.careercore.modules.advisor.vo.ReviewVO;
import com.rickgao.careercore.modules.advisor.vo.TaskVO;
import com.rickgao.careercore.modules.planning.dto.AdoptAdviceRequest;
import com.rickgao.careercore.modules.planning.dto.GoalRequest;
import com.rickgao.careercore.modules.planning.dto.GuidanceRequestPayload;
import com.rickgao.careercore.modules.planning.dto.PlanConfirmRequest;
import com.rickgao.careercore.modules.planning.dto.PlanDraftRequest;
import com.rickgao.careercore.modules.planning.dto.PlanUpdateRequest;
import com.rickgao.careercore.modules.planning.dto.ReviewDraftRequest;
import com.rickgao.careercore.modules.planning.dto.TaskCheckinRequest;
import com.rickgao.careercore.modules.planning.dto.TaskRequest;
import com.rickgao.careercore.modules.planning.dto.TaskStatusUpdate;
import com.rickgao.careercore.modules.planning.service.PlanningService;
import com.rickgao.careercore.modules.planning.vo.GoalVersionVO;
import com.rickgao.careercore.modules.planning.vo.ReminderVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/**
 * 规划模块路由：设目标 / 版本、计划草案与确认、任务打卡、复盘提交与 AI 总结、提醒。
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "目标计划")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    // ==================== 目标 ====================

    @GetMapping("/students/me/goals")
    public ApiResponse<GoalVO> getGoals() {
        return ApiResponse.ok(planningService.getGoals(SecurityUtils.currentUserId()));
    }

    @PostMapping("/students/me/goals")
    public ApiResponse<GoalVO> setGoal(@Valid @RequestBody GoalRequest req) {
        return ApiResponse.ok(planningService.setGoal(SecurityUtils.currentUserId(), req));
    }

    @PutMapping("/students/me/goals")
    public ApiResponse<GoalVO> changeGoal(@Valid @RequestBody GoalRequest req) {
        return ApiResponse.ok(planningService.changeGoal(SecurityUtils.currentUserId(), req));
    }

    @GetMapping("/students/me/goals/versions")
    public ApiResponse<List<GoalVersionVO>> listGoalVersions() {
        return ApiResponse.ok(planningService.listGoalVersions(SecurityUtils.currentUserId()));
    }

    // ==================== 计划 ====================

    @GetMapping("/students/me/plans/latest")
    public ApiResponse<PlanVO> getLatestPlan() {
        return ApiResponse.ok(planningService.getLatestPlan(SecurityUtils.currentUserId()));
    }

    @GetMapping("/students/me/plans")
    public ApiResponse<List<PlanVO>> listPlans() {
        return ApiResponse.ok(planningService.listPlans(SecurityUtils.currentUserId()));
    }

    @GetMapping("/students/me/plans/{planId}")
    public ApiResponse<PlanVO> getPlan(@PathVariable String planId) {
        return ApiResponse.ok(planningService.getPlan(SecurityUtils.currentUserId(), planId));
    }

    @PostMapping("/students/me/plans/draft")
    public ApiResponse<PlanVO> generateDraft(@Valid @RequestBody PlanDraftRequest req) {
        return ApiResponse.ok(planningService.generateDraft(SecurityUtils.currentUserId(), req));
    }

    @PostMapping("/students/me/plans/confirm")
    public ApiResponse<PlanVO> confirmPlan(@Valid @RequestBody PlanConfirmRequest req) {
        return ApiResponse.ok(planningService.confirmPlan(SecurityUtils.currentUserId(), req));
    }

    @PutMapping("/students/me/plans")
    public ApiResponse<PlanVO> updatePlan(@Valid @RequestBody PlanUpdateRequest req) {
        return ApiResponse.ok(planningService.updatePlan(SecurityUtils.currentUserId(), req));
    }

    // ==================== 任务 ====================

    @GetMapping("/students/me/tasks")
    public ApiResponse<PageResult<TaskVO>> listTasks(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(planningService.listTasks(SecurityUtils.currentUserId(), month, status, page, size));
    }

    @GetMapping("/students/me/tasks/{taskId}")
    public ApiResponse<TaskVO> getTask(@PathVariable String taskId) {
        return ApiResponse.ok(planningService.getTask(SecurityUtils.currentUserId(), taskId));
    }

    @PostMapping("/students/me/tasks")
    public ApiResponse<TaskVO> createTask(@Valid @RequestBody TaskRequest req) {
        return ApiResponse.ok(planningService.createTask(SecurityUtils.currentUserId(), req));
    }

    @PutMapping("/students/me/tasks/{taskId}")
    public ApiResponse<TaskVO> updateTask(@PathVariable String taskId,
                                          @Valid @RequestBody TaskStatusUpdate req) {
        return ApiResponse.ok(planningService.updateTask(SecurityUtils.currentUserId(), taskId, req));
    }

    @PostMapping("/students/me/tasks/{taskId}/checkin")
    public ApiResponse<TaskVO> checkinTask(@PathVariable String taskId,
                                           @Valid @RequestBody TaskCheckinRequest req) {
        return ApiResponse.ok(planningService.checkinTask(SecurityUtils.currentUserId(), taskId, req));
    }

    // ==================== 复盘 ====================

    @Tag(name = "阶段复盘")
    @GetMapping("/reviews")
    public ApiResponse<List<ReviewVO>> listReviews() {
        return ApiResponse.ok(planningService.listReviews(SecurityUtils.currentUserId()));
    }

    @Tag(name = "阶段复盘")
    @GetMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewVO> getReview(@PathVariable String reviewId) {
        return ApiResponse.ok(planningService.getReview(SecurityUtils.currentUserId(), reviewId));
    }

    @Tag(name = "阶段复盘")
    @PostMapping("/reviews/drafts")
    public ApiResponse<ReviewVO> createReviewDraft(@Valid @RequestBody ReviewDraftRequest req) {
        return ApiResponse.ok(planningService.createReviewDraft(SecurityUtils.currentUserId(), req));
    }

    @Tag(name = "阶段复盘")
    @PutMapping("/reviews/{reviewId}/draft")
    public ApiResponse<ReviewVO> updateReviewDraft(@PathVariable String reviewId,
                                                   @Valid @RequestBody ReviewDraftRequest req) {
        return ApiResponse.ok(planningService.updateReviewDraft(SecurityUtils.currentUserId(), reviewId, req));
    }

    @Tag(name = "阶段复盘")
    @PostMapping("/reviews/{reviewId}/submit")
    public ApiResponse<ReviewVO> submitReview(@PathVariable String reviewId) {
        return ApiResponse.ok(planningService.submitReview(SecurityUtils.currentUserId(), reviewId));
    }

    @Tag(name = "阶段复盘")
    @PostMapping("/reviews/{reviewId}/ai-summary")
    public ApiResponse<ReviewVO> summarizeReview(@PathVariable String reviewId) {
        return ApiResponse.ok(planningService.summarizeReview(SecurityUtils.currentUserId(), reviewId));
    }

    @Tag(name = "阶段复盘")
    @PostMapping("/reviews/{reviewId}/adopt-advice")
    public ApiResponse<ReviewVO> adoptAdvice(@PathVariable String reviewId,
                                             @Valid @RequestBody AdoptAdviceRequest req) {
        return ApiResponse.ok(planningService.adoptAdvice(SecurityUtils.currentUserId(), reviewId, req));
    }

    @Tag(name = "阶段复盘")
    @PostMapping("/reviews/{reviewId}/guidance-request")
    public ApiResponse<ReviewVO> requestGuidance(@PathVariable String reviewId,
                                                 @Valid @RequestBody GuidanceRequestPayload req) {
        return ApiResponse.ok(planningService.requestGuidance(SecurityUtils.currentUserId(), reviewId, req));
    }

    // ==================== 提醒 ====================

    @GetMapping("/students/me/reminders")
    public ApiResponse<PageResult<ReminderVO>> listReminders(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(planningService.listReminders(SecurityUtils.currentUserId(), unreadOnly, page, size));
    }

    @GetMapping("/students/me/reminders/unread-count")
    public ApiResponse<Map<String, Integer>> unreadCount() {
        return ApiResponse.ok(Map.of("count", planningService.unreadReminderCount(SecurityUtils.currentUserId())));
    }

    @PostMapping("/students/me/reminders/{reminderId}/read")
    public ApiResponse<Map<String, Object>> markRead(@PathVariable String reminderId) {
        planningService.markReminderRead(SecurityUtils.currentUserId(), reminderId);
        return ApiResponse.ok(Map.of());
    }

    @PostMapping("/students/me/reminders/generate")
    public ApiResponse<List<ReminderVO>> generateReminders() {
        return ApiResponse.ok(planningService.generateReminders(SecurityUtils.currentUserId()));
    }
}
