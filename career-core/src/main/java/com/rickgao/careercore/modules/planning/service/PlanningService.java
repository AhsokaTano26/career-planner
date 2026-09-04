package com.rickgao.careercore.modules.planning.service;

import com.rickgao.careercore.common.page.PageResult;
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
import com.rickgao.careercore.modules.planning.vo.GoalVersionVO;
import com.rickgao.careercore.modules.planning.vo.ReminderVO;

import java.util.List;

/**
 * 规划模块业务接口：设目标 / 版本、生成计划草案、确认计划、任务打卡、复盘提交与 AI 总结、提醒生成。
 */
public interface PlanningService {

    // ---------- 目标 ----------

    GoalVO getGoals(String studentId);

    GoalVO setGoal(String studentId, GoalRequest req);

    GoalVO changeGoal(String studentId, GoalRequest req);

    List<GoalVersionVO> listGoalVersions(String studentId);

    // ---------- 计划 ----------

    PlanVO getLatestPlan(String studentId);

    PlanVO getPlan(String studentId, String planId);

    List<PlanVO> listPlans(String studentId);

    PlanVO generateDraft(String studentId, PlanDraftRequest req);

    PlanVO confirmPlan(String studentId, PlanConfirmRequest req);

    PlanVO updatePlan(String studentId, PlanUpdateRequest req);

    // ---------- 任务 ----------

    PageResult<TaskVO> listTasks(String studentId, String month, String status, int page, int size);

    TaskVO getTask(String studentId, String taskId);

    TaskVO createTask(String studentId, TaskRequest req);

    TaskVO updateTask(String studentId, String taskId, TaskStatusUpdate req);

    TaskVO checkinTask(String studentId, String taskId, TaskCheckinRequest req);

    // ---------- 复盘 ----------

    ReviewVO getReview(String studentId, String reviewId);

    List<ReviewVO> listReviews(String studentId);

    ReviewVO createReviewDraft(String studentId, ReviewDraftRequest req);

    ReviewVO updateReviewDraft(String studentId, String reviewId, ReviewDraftRequest req);

    ReviewVO submitReview(String studentId, String reviewId);

    ReviewVO summarizeReview(String studentId, String reviewId);

    ReviewVO adoptAdvice(String studentId, String reviewId, AdoptAdviceRequest req);

    ReviewVO requestGuidance(String studentId, String reviewId, GuidanceRequestPayload req);

    // ---------- 提醒 ----------

    PageResult<ReminderVO> listReminders(String studentId, boolean unreadOnly, int page, int size);

    int unreadReminderCount(String studentId);

    void markReminderRead(String studentId, String reminderId);

    List<ReminderVO> generateReminders(String studentId);
}

