package com.career.core.modules.planning;

import com.career.core.common.BadRequestException;
import com.career.core.common.NotFoundException;
import com.career.core.modules.recommendation.CareerDirection;
import com.career.core.modules.recommendation.RecommendationDao;
import com.career.core.modules.recommendation.RecommendationRunDto;
import com.career.core.modules.recommendation.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 计划模块服务（按线上 Apifox「目标计划」Schema 实现）。
 * <p>
 * Demo 精简逻辑说明：
 *   1. Plan/Goal/Task 响应结构已对齐线上 Schema（id 为字符串、含版本/目标摘要/学期目标/月度任务等字段）；
 *      部分字段（version 等）由 id 或固定模板推导（无独立版本表，注释标注 Demo 精简点）。
 *   2. 任务模板生成（不调用大模型），useAi=true 亦回退模板（后续迭代替换为 career-ai plan_generator）。
 *   3. 列表类接口按线上 200 schema（单个对象）返回最近一条，保证契约一致。
 */
@Service
public class PlanningService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String DEFAULT_SEMESTER = "2026-2027学年第1学期";

    /** 线上方向编码英文关键词 → 本地方向名称关键词映射（Demo 兼容点，见 matchDirectionByKeywords） */
    private static final List<String[]> DIRECTION_KEYWORD_MAP = List.of(
            new String[]{"frontend", "前端"},
            new String[]{"backend", "开发"},
            new String[]{"software", "软件"},
            new String[]{"employment", "开发"},
            new String[]{"data", "数据"},
            new String[]{"analysis", "数据"},
            new String[]{"algorithm", "算法"},
            new String[]{"machine", "算法"},
            new String[]{"game", "游戏"},
            new String[]{"security", "安全"},
            new String[]{"network", "网络"},
            new String[]{"test", "测试"},
            new String[]{"quality", "测试"},
            new String[]{"product", "产品"},
            new String[]{"project", "项目"});

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

    // ---------- 计划草案 ----------

    /** 生成学期计划草案（POST /plans/draft） */
    public PlanDraftDto generatePlanDraft(Long studentId, String directionId, boolean useAi) {
        // Demo 兼容点：directionId 支持本地 DIR 编码与线上语义编码（如 employment_backend），空则回退最新推荐
        CareerDirection direction = resolveDirectionWithFallback(directionId, studentId, 0);
        if (direction == null) {
            throw new BadRequestException("无法确定目标方向，请传入 directionId 或先生成推荐");
        }

        String goalSummary = "本学期围绕【" + direction.name() + "】方向打好基础："
                + "巩固核心课程，完成 1 个可运行的小项目，并在过程中明确后续主攻领域。";
        long goalId = planningDao.insertGoal(studentId, direction.id(),
                "以「" + direction.name() + "」为主攻方向的发展计划", "MAIN", "ACTIVE");
        long planId = planningDao.insertPlan(studentId, goalId, DEFAULT_SEMESTER,
                useAi ? "AI" : "TEMPLATE", "DRAFT");

        List<PlanDraftDto.MonthlyTaskDto> monthlyTasks = buildMonthlyTasks(planId, direction.type());

        return new PlanDraftDto(
                goalSummary,
                List.of(
                        new PlanDraftDto.SemesterGoalDto("巩固专业核心课程与编程基础", "programming_basic"),
                        new PlanDraftDto.SemesterGoalDto("完成 1 个可运行的小项目", "project_basic")),
                monthlyTasks,
                List.of("任务可随课程安排与兴趣变化调整。"));
    }

    private List<PlanDraftDto.MonthlyTaskDto> buildMonthlyTasks(long planId, String type) {
        List<String[]> template = taskTemplatesFor(type);
        LocalDate month = LocalDate.of(2026, 9, 1);
        List<PlanDraftDto.MonthlyTaskDto> tasks = new ArrayList<>();
        for (int i = 0; i < template.size(); i++) {
            String[] item = template.get(i);
            String m = month.plusMonths(i).format(MONTH_FMT);
            planningDao.insertPlanTask(planId, m, item[0], "TODO");
            tasks.add(new PlanDraftDto.MonthlyTaskDto(m, item[0], item[1], Double.parseDouble(item[2])));
        }
        return tasks;
    }

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

    // ---------- 目标 ----------

    /** 我的目标（GET /goals）：线上 200 schema 为单个 Goal（主/备选）。无目标返回 null（Controller 转 404） */
    public GoalDto getGoals(Long studentId) {
        List<PlanningDao.GoalRow> goals = planningDao.findGoals(studentId);
        if (goals.isEmpty()) {
            return null;
        }
        return toGoalDto(goals);
    }

    /** 设置 / 变更目标（POST /goals）：入参为方向编码（兼容线上语义编码），落库后返回 Goal */
    public GoalDto setGoal(Long studentId, String primaryDirectionId, String backupDirectionId) {
        CareerDirection primary = resolveDirectionWithFallback(primaryDirectionId, studentId, 0);
        if (primary == null) {
            throw new BadRequestException("方向不存在：" + primaryDirectionId);
        }
        GoalDto.GoalItemDto primaryItem = toGoalItem(primary);
        planningDao.insertGoal(studentId, primary.id(),
                primaryItem.name() == null ? "我的发展目标" : primaryItem.name(), "MAIN", "ACTIVE");

        GoalDto.GoalItemDto backupItem = null;
        if (backupDirectionId != null && !backupDirectionId.isBlank()) {
            CareerDirection backup = resolveDirectionWithFallback(backupDirectionId, studentId, 1);
            if (backup != null) {
                backupItem = toGoalItem(backup);
                planningDao.insertGoal(studentId, backup.id(), backup.name(), "BACKUP", "ACTIVE");
            }
        }
        // Demo 精简点：未提供/未解析出备选目标时以主目标兜底，保证 backup 非 null（契约不允许 null）
        if (backupItem == null) {
            backupItem = primaryItem;
        }
        return new GoalDto(primaryItem, backupItem, "G-v1",
                LocalDate.now().atStartOfDay().toString());
    }

    /** 目标版本历史（GET /goal-versions）：Demo 精简，返回当前目标（无独立版本表） */
    public GoalDto getGoalVersions(Long studentId) {
        return getGoals(studentId);
    }

    private GoalDto toGoalDto(List<PlanningDao.GoalRow> goals) {
        PlanningDao.GoalRow primary = goals.stream()
                .filter(g -> "MAIN".equalsIgnoreCase(g.goalType()))
                .findFirst().orElse(goals.get(0));
        PlanningDao.GoalRow backup = goals.stream()
                .filter(g -> "BACKUP".equalsIgnoreCase(g.goalType()))
                .findFirst().orElse(null);
        CareerDirection primaryDir = primary.directionId() == null ? null
                : recommendationDao.findDirectionByCode(codeOf(primary.directionId()));
        GoalDto.GoalItemDto primaryItem = new GoalDto.GoalItemDto(
                primary.directionId() == null ? null : codeOf(primary.directionId()),
                primaryDir == null ? primary.title() : primaryDir.name(),
                primary.createdAt() == null ? null : primary.createdAt().toString());
        GoalDto.GoalItemDto backupItem = null;
        if (backup != null) {
            CareerDirection backupDir = backup.directionId() == null ? null
                    : recommendationDao.findDirectionByCode(codeOf(backup.directionId()));
            backupItem = new GoalDto.GoalItemDto(
                    backup.directionId() == null ? null : codeOf(backup.directionId()),
                    backupDir == null ? backup.title() : backupDir.name(),
                    backup.createdAt() == null ? null : backup.createdAt().toString());
        }
        return new GoalDto(primaryItem, backupItem, "G-v1",
                primary.createdAt() == null ? null : primary.createdAt().toString());
    }

    private CareerDirection resolveDirection(String directionId) {
        if (directionId == null || directionId.isBlank()) {
            return null;
        }
        return recommendationDao.findDirectionByCode(directionId);
    }

    private GoalDto.GoalItemDto toGoalItem(CareerDirection direction) {
        if (direction == null) {
            return new GoalDto.GoalItemDto(null, null, null);
        }
        return new GoalDto.GoalItemDto(direction.directionCode(), direction.name(),
                LocalDate.now().atStartOfDay().toString());
    }

    /** 解析方向：本地 DIR 编码 → 线上语义编码关键词匹配 → 最新推荐回退（Demo 兼容点，见各步注释） */
    private CareerDirection resolveDirectionWithFallback(String directionId, Long studentId, int fallbackIndex) {
        CareerDirection dir = resolveDirection(directionId);
        if (dir == null) {
            dir = matchDirectionByKeywords(directionId);
        }
        if (dir == null) {
            dir = latestRecommendedDirection(studentId, fallbackIndex);
        }
        return dir;
    }

    /**
     * 线上方向编码 → 本地方向（Demo 兼容点：线上编码如 employment_backend / data_analysis 不在本地
     * direction_code 中，按英文关键词映射到中文名称/类型粗匹配；匹配不到返回 null）。
     */
    private CareerDirection matchDirectionByKeywords(String directionId) {
        if (directionId == null || directionId.isBlank()) {
            return null;
        }
        String lower = directionId.toLowerCase();
        List<CareerDirection> all = recommendationDao.findAllDirections();
        // 1) 名称/类型直接包含编码片段
        for (CareerDirection d : all) {
            String name = d.name() == null ? "" : d.name().toLowerCase();
            String type = d.type() == null ? "" : d.type().toLowerCase();
            if (name.contains(lower) || type.contains(lower)) {
                return d;
            }
        }
        // 2) 英文关键词 → 中文名称关键词
        for (String[] kv : DIRECTION_KEYWORD_MAP) {
            if (lower.contains(kv[0])) {
                for (CareerDirection d : all) {
                    String name = d.name() == null ? "" : d.name();
                    if (name.contains(kv[1])) {
                        return d;
                    }
                }
            }
        }
        return null;
    }

    /** 回退：取学生最新推荐批次第 index 个结果对应方向（保证契约非空；无推荐返回 null） */
    private CareerDirection latestRecommendedDirection(Long studentId, int index) {
        RecommendationRunDto latest = recommendationService.getLatest(studentId);
        if (latest == null || latest.results() == null || latest.results().isEmpty()) {
            return null;
        }
        int idx = Math.min(index, latest.results().size() - 1);
        return resolveDirection(latest.results().get(idx).directionId());
    }

    // ---------- 计划 ----------

    /** 最新计划（GET /plans/latest）：无则返回 null（Controller 转 404） */
    public PlanDto getLatestPlan(Long studentId) {
        List<PlanningDao.PlanRow> plans = planningDao.findPlans(studentId);
        if (plans.isEmpty()) {
            return null;
        }
        return toPlanDto(plans.get(0));
    }

    /** 编辑计划（PATCH /plans/{planId}）：Demo 仅支持学期更新，其余字段回读 */
    public PlanDto editPlan(Long planId, String semester) {
        PlanningDao.PlanRow plan = requirePlan(planId);
        String sem = (semester != null && !semester.isBlank()) ? semester : plan.semester();
        planningDao.updatePlan(planId, sem, plan.status());
        return toPlanDto(new PlanningDao.PlanRow(plan.id(), plan.goalId(), sem, plan.source(),
                plan.status(), plan.createdAt()));
    }

    /** 编辑最新计划（PATCH /plans 空 planId 兜底）：无计划则 404；仅支持 semester 更新，其余字段回读 */
    public PlanDto editLatestPlan(Long studentId, String semester) {
        List<PlanningDao.PlanRow> plans = planningDao.findPlans(studentId);
        if (plans.isEmpty()) {
            throw new NotFoundException("暂无计划，请先生成计划草案");
        }
        return editPlan(plans.get(0).id(), semester);
    }

    /** 确认计划（POST /plans/{planId}/confirm） */
    public PlanDto confirmPlan(Long planId) {
        PlanningDao.PlanRow plan = requirePlan(planId);
        planningDao.updatePlan(planId, plan.semester(), "CONFIRMED");
        return toPlanDto(new PlanningDao.PlanRow(plan.id(), plan.goalId(), plan.semester(), plan.source(),
                "CONFIRMED", plan.createdAt()));
    }

    /** 确认最新计划（POST /plans/confirm 空 planId 兜底）：无计划则 404 */
    public PlanDto confirmLatestPlan(Long studentId) {
        List<PlanningDao.PlanRow> plans = planningDao.findPlans(studentId);
        if (plans.isEmpty()) {
            throw new NotFoundException("暂无计划，请先生成计划草案");
        }
        return confirmPlan(plans.get(0).id());
    }

    /** 计划版本历史（GET /plan-versions）：线上 200 schema 为单个 Plan，返回最近一条 */
    public PlanDto getPlanVersions(Long studentId) {
        return getLatestPlan(studentId);
    }

    private PlanDto toPlanDto(PlanningDao.PlanRow plan) {
        List<PlanningDao.PlanTaskRow> tasks = plan.id() == null ? List.of()
                : planningDao.findPlanTasks(plan.id());
        List<PlanDto.MonthlyTaskDto> monthly = tasks.stream()
                .map(t -> new PlanDto.MonthlyTaskDto(t.month(), t.title(),
                        taskTypeOf(t.title()), 8.0))
                .toList();
        // Demo 精简点：goalSummary 无独立列，用默认摘要；版本号由 id 推导
        return new PlanDto(
                plan.id() == null ? null : "PLAN-" + plan.id(),
                "P-v1",
                plan.status(),
                plan.source(),
                "本学期围绕主攻方向打好基础，完成阶段性目标。",
                List.of(new PlanDto.SemesterGoalDto("完成主攻方向的基础学习", "programming_basic")),
                monthly,
                List.of("任务可随课程安排与兴趣变化调整。"),
                plan.createdAt() == null ? null : plan.createdAt().toString(),
                plan.createdAt() == null ? null : plan.createdAt().toString());
    }

    private String taskTypeOf(String title) {
        if (title == null) {
            return "LEARNING";
        }
        if (title.contains("复盘")) {
            return "REVIEW";
        }
        if (title.contains("项目") || title.contains("实践")) {
            return "PRACTICE";
        }
        return "LEARNING";
    }

    private PlanningDao.PlanRow requirePlan(Long planId) {
        PlanningDao.PlanRow plan = planningDao.findPlanById(planId);
        if (plan == null) {
            throw new NotFoundException("计划不存在：" + planId);
        }
        return plan;
    }

    // ---------- 任务 ----------

    /** 任务列表（GET /tasks）：线上 200 schema 为单个 Task，返回最近一条 */
    public TaskDto getTasks(Long studentId) {
        List<PlanningDao.TaskRow> tasks = planningDao.findTasks(studentId);
        if (tasks.isEmpty()) {
            return null;
        }
        return toTaskDto(tasks.get(0));
    }

    /** 新增任务（POST /tasks） */
    public TaskDto addTask(Long studentId, String title, String month) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("任务标题不能为空");
        }
        long id = planningDao.insertTask(studentId, title, month);
        return toTaskDto(planningDao.findTaskById(id));
    }

    /** 更新任务（PATCH /tasks/{taskId}） */
    public TaskDto updateTask(Long taskId, String title, String status) {
        PlanningDao.TaskRow task = requireTask(taskId);
        String t = (title != null && !title.isBlank()) ? title : task.title();
        String s = (status != null && !status.isBlank()) ? status : task.status();
        planningDao.updateTask(taskId, t, s);
        return toTaskDto(new PlanningDao.TaskRow(taskId, task.studentId(), t, s,
                task.month(), task.createdAt()));
    }

    /** 更新最新任务（PATCH /tasks 空 taskId 兜底）：无任务则 404 */
    public TaskDto updateLatestTask(Long studentId, String title, String status) {
        List<PlanningDao.TaskRow> tasks = planningDao.findTasks(studentId);
        if (tasks.isEmpty()) {
            throw new NotFoundException("暂无任务");
        }
        return updateTask(tasks.get(0).id(), title, status);
    }

    /** 删除任务（DELETE /tasks/{taskId}） */
    public void deleteTask(Long taskId) {
        requireTask(taskId);
        planningDao.deleteTask(taskId);
    }

    /** 删除最新任务（DELETE /tasks 空 taskId 兜底）：无任务则 404 */
    public void deleteLatestTask(Long studentId) {
        List<PlanningDao.TaskRow> tasks = planningDao.findTasks(studentId);
        if (tasks.isEmpty()) {
            throw new NotFoundException("暂无任务");
        }
        deleteTask(tasks.get(0).id());
    }

    /** 任务打卡（POST /tasks/{taskId}/checkin）：置为 DONE，返回契约 TaskCheckin 扁平结构 */
    public TaskDto.TaskCheckinDto checkinTask(Long taskId, Map<String, Object> body) {
        PlanningDao.TaskRow task = requireTask(taskId);
        planningDao.updateTask(taskId, task.title(), "DONE");
        return buildCheckin(taskId, task, body);
    }

    /** 对最新任务打卡（POST /tasks/checkin 空 taskId 兜底）：无任务则 404 */
    public TaskDto.TaskCheckinDto checkinLatestTask(Long studentId, Map<String, Object> body) {
        List<PlanningDao.TaskRow> tasks = planningDao.findTasks(studentId);
        if (tasks.isEmpty()) {
            throw new NotFoundException("暂无任务");
        }
        PlanningDao.TaskRow task = tasks.get(0);
        return checkinTask(task.id(), body);
    }

    /** 组装契约 TaskCheckin：id/taskId/doneDesc 必填，其余字段非 null 兜底 */
    private TaskDto.TaskCheckinDto buildCheckin(Long taskId, PlanningDao.TaskRow task, Map<String, Object> body) {
        String id = "TC-" + taskId;
        String taskIdStr = "T" + taskId;
        String doneDesc = body == null ? null : (String) body.get("doneDesc");
        String gains = body == null ? null : (String) body.get("gains");
        String difficulties = body == null ? null : (String) body.get("difficulties");
        String proofUrl = body == null ? null : (String) body.get("proofUrl");
        // Demo 精简点：doneDesc 缺省取“任务已完成”，可选字段缺省空串，保证契约非空；后续迭代替换为真实打卡表
        String checkedInAt = task.createdAt() == null ? LocalDateTime.now().toString() : task.createdAt().toString();
        return new TaskDto.TaskCheckinDto(
                id,
                taskIdStr,
                (doneDesc == null || doneDesc.isBlank()) ? "任务已完成" : doneDesc,
                gains == null ? "" : gains,
                difficulties == null ? "" : difficulties,
                proofUrl == null ? "" : proofUrl,
                checkedInAt);
    }

    private TaskDto toTaskDto(PlanningDao.TaskRow task) {
        String status = normalizeTaskStatus(task.status());
        String id = task.id() == null ? null : "T" + task.id();
        // Demo 精简点：无独立 month/deadline/note/打卡列，字段以默认值兜底，保证契约非空；后续迭代替换为真实列
        String month = task.month() == null || task.month().isBlank() ? defaultMonth() : task.month();
        String checkedInAt = task.createdAt() == null ? LocalDateTime.now().toString() : task.createdAt().toString();
        return new TaskDto(
                id,
                month,
                task.title(),
                taskTypeOf(task.title()),
                0.0,
                status,
                lastDayOfMonth(month),
                List.of(),
                "",
                checkedInAt,
                new TaskDto.TaskCheckinDto(
                        id == null ? null : "TC-" + task.id(),
                        id,
                        "DONE".equals(status) ? "任务已完成" : "",
                        "",
                        "",
                        "",
                        checkedInAt));
    }

    /** 默认月份（yyyy-MM）：任务无月份时的 Demo 兜底 */
    private String defaultMonth() {
        return LocalDate.now().format(MONTH_FMT);
    }

    /** 月份最后一天（deadline 兜底）：如 2026-09 → 2026-09-30 */
    private String lastDayOfMonth(String month) {
        try {
            return YearMonth.parse(month).atEndOfMonth().toString();
        } catch (Exception e) {
            return LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1).toString();
        }
    }

    /** 本地任务状态 → 线上枚举（TODO/DOING/DONE → PENDING/DOING/DONE） */
    private String normalizeTaskStatus(String status) {
        if (status == null) {
            return "PENDING";
        }
        return switch (status.toUpperCase()) {
            case "TODO" -> "PENDING";
            case "DOING" -> "DOING";
            case "DONE" -> "DONE";
            case "DELAY" -> "DELAYED";
            case "ABANDON" -> "ABANDONED";
            default -> status.toUpperCase();
        };
    }

    private PlanningDao.TaskRow requireTask(Long taskId) {
        PlanningDao.TaskRow task = planningDao.findTaskById(taskId);
        if (task == null) {
            throw new NotFoundException("任务不存在：" + taskId);
        }
        return task;
    }

    // ---------- 提醒 ----------

    /** 站内提醒（GET /reminders）：线上 200 schema 为单个 Reminder，由 TODO 任务推导最近一条 */
    public ReminderDto getReminders(Long studentId) {
        List<PlanningDao.TaskRow> tasks = planningDao.findTasks(studentId);
        return tasks.stream()
                .filter(t -> !"DONE".equalsIgnoreCase(t.status()))
                .sorted(Comparator.comparing(PlanningDao.TaskRow::id).reversed())
                .findFirst()
                .map(t -> new ReminderDto(
                        "RM-" + t.id(),
                        "TASK_DEADLINE",
                        "任务待完成：" + t.title(),
                        "请及时更新任务状态并完成阶段复盘。",
                        false,
                        t.createdAt() == null ? null : t.createdAt().toString()))
                .orElse(null);
    }

    private String codeOf(Long directionId) {
        CareerDirection d = recommendationDao.findAllDirections().stream()
                .filter(x -> x.id().equals(directionId))
                .findFirst().orElse(null);
        return d == null ? String.valueOf(directionId) : d.directionCode();
    }
}
