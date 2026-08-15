package com.career.core.modules.planning;

import com.career.core.common.BadRequestException;
import com.career.core.common.NotFoundException;
import com.career.core.modules.recommendation.CareerDirection;
import com.career.core.modules.recommendation.RecommendationDao;
import com.career.core.modules.recommendation.RecommendationRunDto;
import com.career.core.modules.recommendation.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计划模块服务（按线上 Apifox「目标计划」PlanDraft 结构实现）。
 * <p>
 * Demo 精简逻辑说明：
 *   1. 数据来源：优先请求体 directionId；未传时调用推荐接口（/students/me/recommendations/latest）取排名第一方向。
 *   2. Demo 阶段由规则模板生成任务（不调用大模型），useAi=true 亦回退模板（后续迭代替换为 career-ai plan_generator）。
 *   3. 草案落库（student_goal / semester_plan / plan_task）后返回，不做异步处理。
 */
@Service
public class PlanningService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String DEFAULT_SEMESTER = "2026-2027学年第1学期";

    private final RecommendationService recommendationService;
    private final RecommendationDao recommendationDao;
    private final PlanningDao planningDao;

    public PlanningService(RecommendationService recommendationService,
                           RecommendationDao recommendationDao,
                           PlanningDao planningDao) {
        this.recommendationService = recommendationService;
        this.recommendationDao = recommendationDao;
        this.planningDao = planningDao;
    }

    /**
     * 生成学期计划草案。
     *
     * @param studentId   学生ID
     * @param directionId 目标方向编码（可空：为空则取最新推荐第一名）
     * @param useAi       是否调用 AI（Demo 阶段忽略，规则模板）
     * @return PlanDraftDto；无法确定目标方向时返回 null（Controller 转 400）
     */
    public PlanDraftDto generatePlanDraft(Long studentId, String directionId, boolean useAi) {
        // 1. 确定目标方向编码：优先入参；否则取最新推荐第一名
        String dirCode = directionId;
        if (dirCode == null || dirCode.isBlank()) {
            RecommendationRunDto latest = recommendationService.getLatest(studentId);
            if (latest != null && latest.results() != null && !latest.results().isEmpty()) {
                dirCode = latest.results().get(0).directionId();
            }
        }
        if (dirCode == null || dirCode.isBlank()) {
            throw new BadRequestException("无法确定目标方向，请传入 directionId 或先生成推荐");
        }
        CareerDirection direction = recommendationDao.findDirectionByCode(dirCode);
        if (direction == null) {
            throw new BadRequestException("方向不存在：" + dirCode);
        }

        // 2. 以目标方向生成主目标并落库（direction_id 关联 career_direction.id）
        String goalSummary = "本学期围绕【" + direction.name() + "】方向打好基础："
                + "巩固核心课程，完成 1 个可运行的小项目，并在过程中明确后续主攻领域。";
        long goalId = planningDao.insertGoal(studentId, direction.id(), "以「" + direction.name() + "」为主攻方向的发展计划",
                "MAIN", "ACTIVE");
        long planId = planningDao.insertPlan(studentId, goalId, DEFAULT_SEMESTER, useAi ? "AI" : "TEMPLATE", "DRAFT");

        // 3. 套用任务模板生成月度任务并落库
        List<PlanDraftDto.MonthlyTaskDto> monthlyTasks = buildMonthlyTasks(planId, direction.type());

        // 4. 组装 PlanDraft 响应
        return new PlanDraftDto(
                goalSummary,
                List.of(
                        new PlanDraftDto.SemesterGoalDto("巩固专业核心课程与编程基础", "programming_basic"),
                        new PlanDraftDto.SemesterGoalDto("完成 1 个可运行的小项目", "project_basic")),
                monthlyTasks,
                List.of("任务可随课程安排与兴趣变化调整。"));
    }

    /** 月度任务（按方向类型选择模板，Demo 预置；后续替换为大模型生成） */
    private List<PlanDraftDto.MonthlyTaskDto> buildMonthlyTasks(long planId, String type) {
        List<String[]> template = taskTemplatesFor(type);
        LocalDate month = LocalDate.of(2026, 9, 1);
        List<PlanDraftDto.MonthlyTaskDto> tasks = new ArrayList<>();
        for (int i = 0; i < template.size(); i++) {
            String[] item = template.get(i);
            String m = month.plusMonths(i).format(MONTH_FMT);
            planningDao.insertTask(planId, m, item[0], "TODO");
            tasks.add(new PlanDraftDto.MonthlyTaskDto(m, item[0], item[1], Double.parseDouble(item[2])));
        }
        return tasks;
    }

    /** 任务模板：[标题, 任务类型, 预计小时数] */
    private List<String[]> taskTemplatesFor(String type) {
        if (type == null) {
            return List.of(
                    new String[]{"明确主攻方向并制定学习路线", "CAREER", "6"},
                    new String[]{"完成一项实践项目", "PRACTICE", "12"},
                    new String[]{"阶段复盘与调整", "REVIEW", "4"});
        }
        return switch (type) {
            case "数据算法" -> List.of(
                    new String[]{"学习 Python 与数据分析基础", "LEARNING", "12"},
                    new String[]{"完成一个数据分析小项目", "PRACTICE", "12"},
                    new String[]{"系统学习机器学习入门", "LEARNING", "10"},
                    new String[]{"阶段复盘并确定下一步", "REVIEW", "4"});
            case "技术研发" -> List.of(
                    new String[]{"巩固编程语言与数据结构基础", "LEARNING", "12"},
                    new String[]{"完成一个课程/开源项目", "PRACTICE", "12"},
                    new String[]{"学习主攻方向核心框架", "LEARNING", "10"},
                    new String[]{"阶段复盘并明确主攻领域", "REVIEW", "4"});
            case "产品管理" -> List.of(
                    new String[]{"学习产品方法与需求分析", "LEARNING", "8"},
                    new String[]{"完成一份产品分析报告", "PRACTICE", "8"},
                    new String[]{"参与一次项目协作实践", "PRACTICE", "10"},
                    new String[]{"阶段复盘并调整计划", "REVIEW", "4"});
            default -> List.of(
                    new String[]{"明确主攻方向并制定学习路线", "CAREER", "6"},
                    new String[]{"完成一项实践项目", "PRACTICE", "12"},
                    new String[]{"阶段复盘与调整", "REVIEW", "4"});
        };
    }

    // ---------- 目标 / 计划 / 任务（补齐线上「目标计划」模块） ----------

    /** 我的目标列表 */
    public List<GoalDto> getGoals(Long studentId) {
        return planningDao.findGoals(studentId);
    }

    /** 设置 / 变更目标 */
    public GoalDto setGoal(Long studentId, String directionId, String title, String goalType) {
        Long dirId = null;
        if (directionId != null && !directionId.isBlank()) {
            CareerDirection direction = recommendationDao.findDirectionByCode(directionId);
            if (direction != null) {
                dirId = direction.id();
            }
        }
        String goalTitle = (title != null && !title.isBlank()) ? title : "我的发展目标";
        String type = (goalType != null && !goalType.isBlank()) ? goalType : "MAIN";
        long id = planningDao.insertGoal(studentId, dirId, goalTitle, type, "ACTIVE");
        return new GoalDto(id, dirId, goalTitle, type, "ACTIVE");
    }

    /** 最新计划 */
    public PlanDto getLatestPlan(Long studentId) {
        List<PlanDto> plans = planningDao.findPlans(studentId);
        return plans.isEmpty() ? null : plans.get(0);
    }

    /** 编辑计划（Demo：仅学期可改） */
    public PlanDto editPlan(Long planId, String semester) {
        PlanDto plan = requirePlan(planId);
        String sem = (semester != null && !semester.isBlank()) ? semester : plan.semester();
        planningDao.updatePlan(planId, sem, plan.status());
        return new PlanDto(plan.id(), plan.goalId(), sem, plan.source(), plan.status());
    }

    /** 确认计划 */
    public PlanDto confirmPlan(Long planId) {
        PlanDto plan = requirePlan(planId);
        planningDao.updatePlan(planId, plan.semester(), "CONFIRMED");
        return new PlanDto(plan.id(), plan.goalId(), plan.semester(), plan.source(), "CONFIRMED");
    }

    /** 计划版本历史（Demo 精简点：无独立版本表，历史列表即版本） */
    public List<PlanDto> getPlanVersions(Long studentId) {
        return planningDao.findPlans(studentId);
    }

    /** 目标版本历史（Demo 精简点：无独立版本表，历史列表即版本） */
    public List<GoalDto> getGoalVersions(Long studentId) {
        return planningDao.findGoals(studentId);
    }

    /** 任务列表 */
    public List<TaskDto> getTasks(Long studentId) {
        return planningDao.findTasks(studentId);
    }

    /** 新增任务 */
    public TaskDto addTask(Long studentId, String title, String month) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("任务标题不能为空");
        }
        long id = planningDao.insertTask(studentId, title, month);
        return planningDao.findTaskById(id);
    }

    /** 更新任务 */
    public TaskDto updateTask(Long taskId, String title, String status) {
        TaskDto task = requireTask(taskId);
        String t = (title != null && !title.isBlank()) ? title : task.title();
        String s = (status != null && !status.isBlank()) ? status : task.status();
        planningDao.updateTask(taskId, t, s);
        return new TaskDto(taskId, t, s, task.month());
    }

    /** 删除任务 */
    public void deleteTask(Long taskId) {
        requireTask(taskId);
        planningDao.deleteTask(taskId);
    }

    /** 任务打卡：置为 DONE */
    public TaskDto checkinTask(Long taskId) {
        TaskDto task = requireTask(taskId);
        planningDao.updateTask(taskId, task.title(), "DONE");
        return new TaskDto(taskId, task.title(), "DONE", task.month());
    }

    /** 站内提醒（Demo 精简点：无提醒表，由 TODO 任务推导；后续迭代替换为提醒中心） */
    public List<Map<String, Object>> getReminders(Long studentId) {
        List<TaskDto> todos = planningDao.findTasks(studentId).stream()
                .filter(t -> "TODO".equals(t.status()))
                .toList();
        List<Map<String, Object>> reminders = new ArrayList<>();
        for (TaskDto t : todos) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", t.id());
            m.put("message", "任务待完成：" + t.title());
            reminders.add(m);
        }
        return reminders;
    }

    private PlanDto requirePlan(Long planId) {
        PlanDto plan = planningDao.findPlanById(planId);
        if (plan == null) {
            throw new NotFoundException("计划不存在：" + planId);
        }
        return plan;
    }

    private TaskDto requireTask(Long taskId) {
        TaskDto task = planningDao.findTaskById(taskId);
        if (task == null) {
            throw new NotFoundException("任务不存在：" + taskId);
        }
        return task;
    }
}
