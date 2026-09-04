package com.rickgao.careercore.modules.planning.mapper;

import com.rickgao.careercore.modules.planning.entity.GoalVersion;
import com.rickgao.careercore.modules.planning.entity.PlanTask;
import com.rickgao.careercore.modules.planning.entity.PlanVersion;
import com.rickgao.careercore.modules.planning.entity.Reminder;
import com.rickgao.careercore.modules.planning.entity.SemesterPlan;
import com.rickgao.careercore.modules.planning.entity.StageReview;
import com.rickgao.careercore.modules.planning.entity.StudentGoal;
import com.rickgao.careercore.modules.planning.entity.TaskCheckin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 规划模块 Mapper：目标 / 计划 / 任务 / 复盘 / 提醒 的增删改查。
 */
@Mapper
public interface PlanningMapper {

    // ---------- 学生目标 student_goal ----------

    List<StudentGoal> selectGoals(@Param("studentId") String studentId);

    StudentGoal selectGoalById(@Param("id") String id);

    /** 按类型查询单个目标（PRIMARY / BACKUP）。 */
    StudentGoal selectGoalByType(@Param("studentId") String studentId, @Param("goalType") String goalType);

    int insertGoal(StudentGoal goal);

    int updateGoal(StudentGoal goal);

    int disableGoal(@Param("id") String id);

    int countActiveGoals(@Param("studentId") String studentId);

    // ---------- 目标版本 goal_version ----------

    List<GoalVersion> selectGoalVersions(@Param("goalId") String goalId);

    int insertGoalVersion(GoalVersion version);

    // ---------- 学期计划 semester_plan ----------

    SemesterPlan selectPlanById(@Param("id") String id);

    List<SemesterPlan> selectPlansByStudent(@Param("studentId") String studentId);

    SemesterPlan selectLatestPlanByStatus(@Param("studentId") String studentId, @Param("status") String status);

    SemesterPlan selectLatestPlan(@Param("studentId") String studentId);

    int insertPlan(SemesterPlan plan);

    int updatePlan(SemesterPlan plan);

    long countPlans(@Param("studentId") String studentId);

    // ---------- 计划版本 plan_version ----------

    List<PlanVersion> selectPlanVersions(@Param("planId") String planId);

    int insertPlanVersion(PlanVersion version);

    // ---------- 计划任务 plan_task ----------

    PlanTask selectTaskById(@Param("id") String id);

    List<PlanTask> selectTasksByPlan(@Param("planId") String planId);

    List<PlanTask> selectTasksByStudent(@Param("studentId") String studentId,
                                        @Param("month") String month,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("size") int size);

    long countTasks(@Param("studentId") String studentId,
                    @Param("month") String month,
                    @Param("status") String status);

    int insertTask(PlanTask task);

    int updateTask(PlanTask task);

    // ---------- 任务打卡 task_checkin ----------

    TaskCheckin selectCheckinById(@Param("id") String id);

    TaskCheckin selectLatestCheckinByTask(@Param("taskId") String taskId);

    int insertCheckin(TaskCheckin checkin);

    // ---------- 阶段复盘 stage_review ----------

    StageReview selectReviewById(@Param("id") String id);

    List<StageReview> selectReviewsByStudent(@Param("studentId") String studentId);

    StageReview selectLatestReviewByStudent(@Param("studentId") String studentId);

    int insertReview(StageReview review);

    int updateReview(StageReview review);

    // ---------- 站内提醒 reminder ----------

    Reminder selectReminderById(@Param("id") String id);

    List<Reminder> selectRemindersByStudent(@Param("studentId") String studentId,
                                            @Param("unreadOnly") Boolean unreadOnly,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    long countReminders(@Param("studentId") String studentId, @Param("unreadOnly") Boolean unreadOnly);

    int insertReminder(Reminder reminder);

    int markReminderRead(@Param("id") String id);

    int countUnreadReminders(@Param("studentId") String studentId);
}

